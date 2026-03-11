package org.oddlama.vane.core.misc

import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.loot.Lootable
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import java.util.*

class LootChestProtector(context: Context<Core?>?) : Listener<Core?>(context) {
    // Tracks first-break attempts: block -> (playerUUID -> timestamp)
    private val lootBreakAttempts: MutableMap<Block, MutableMap<UUID, Long>> = mutableMapOf()

    @ConfigBoolean(
        def = true,
        desc = "Prevent players from breaking blocks with loot-tables (like treasure chests) when they first attempt to destroy it. They still can break it, but must do so within a short timeframe."
    )
    var configWarnBreakingLootBlocks: Boolean = false

    @LangMessage
    var langBreakLootBlockPrevented: TranslatedMessage? = null

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBreakLootChest(event: BlockBreakEvent) {
        if (!configWarnBreakingLootBlocks) return

        val lootable = event.block.getState(false) as? Lootable ?: return
        if (!lootable.hasLootTable()) return

        val block = event.block
        val player = event.player
        val now = System.currentTimeMillis()

        val blockAttempts = lootBreakAttempts.getOrPut(block) { mutableMapOf() }
        val previousAttempt = blockAttempts[player.uniqueId]

        if (previousAttempt != null) {
            val elapsed = now - previousAttempt
            if (elapsed in 5001L until 30000L) return  // Allow break within the window
        }

        blockAttempts[player.uniqueId] = now
        langBreakLootBlockPrevented!!.send(player)
        event.isCancelled = true
    }
}
