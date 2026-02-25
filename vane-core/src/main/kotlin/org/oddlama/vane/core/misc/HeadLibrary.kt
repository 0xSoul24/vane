package org.oddlama.vane.core.misc

import org.bukkit.Material
import org.bukkit.block.Skull
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.material.HeadMaterialLibrary.fromTexture
import org.oddlama.vane.core.material.HeadMaterialLibrary.load
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.BlockUtil.dropNaturally
import org.oddlama.vane.util.BlockUtil.textureFromSkull
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.stream.Collectors

class HeadLibrary(context: Context<Core?>?) : Listener<Core?>(context) {
    @ConfigBoolean(
        def = true,
        desc = "When a player head is broken by a player that exists in /heads, drop the correctly named item as seen in /heads. You can disable this if it interferes with similarly textured heads from other plugins."
    )
    var configPlayerHeadDrops: Boolean = false

    init {
        // Load a head material library
        module!!.log.info("Loading head library...")
        try {
            var json: String? = null
            module!!.getResource("head_library.json").use { input ->
                if (input != null) {
                    BufferedReader(
                        InputStreamReader(input, StandardCharsets.UTF_8)
                    ).use { reader ->
                        json = reader.lines().collect(Collectors.joining("\n"))
                    }
                }
            }
            if (json == null) {
                throw IOException("Failed to get contents of resource head_library.json")
            }
            load(json)
        } catch (e: IOException) {
            module!!.log.log(Level.SEVERE, "Error while loading head_library.json! Shutting down.", e)
            module!!.server.shutdown()
        }
    }

    // Restore correct head item from a head library when broken
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!configPlayerHeadDrops) {
            return
        }

        val block = event.getBlock()
        if (block.type != Material.PLAYER_HEAD && block.type != Material.PLAYER_WALL_HEAD) {
            return
        }

        val skull = block.state as Skull
        val texture = textureFromSkull(skull) ?: return

        val headMaterial = fromTexture(texture) ?: return

        // Set to air and drop item
        block.type = Material.AIR
        dropNaturally(block, headMaterial.item())
    }
}