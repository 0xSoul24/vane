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
    // Prevent loot chest destruction
    private val lootBreakAttempts: MutableMap<Block?, MutableMap<UUID?, Long?>?> =
        HashMap<Block?, MutableMap<UUID?, Long?>?>()

    // TODO(legacy): this should become a separate group instead of having
    // this boolean.
    @ConfigBoolean(
        def = true,
        desc = "Prevent players from breaking blocks with loot-tables (like treasure chests) when they first attempt to destroy it. They still can break it, but must do so within a short timeframe."
    )
    var configWarnBreakingLootBlocks: Boolean = false

    @LangMessage
    var langBreakLootBlockPrevented: TranslatedMessage? = null

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBreakLootChest(event: BlockBreakEvent) {
        if (!configWarnBreakingLootBlocks) {
            return
        }

        val state = event.getBlock().getState(false)
        if (state !is Lootable) {
            return
        }

        val lootable = state as Lootable
        if (!lootable.hasLootTable()) {
            return
        }

        val block = event.getBlock()
        val player = event.player
        var blockAttempts = lootBreakAttempts[block]
        val now = System.currentTimeMillis()
        if (blockAttempts != null) {
            val playerAttemptTime = blockAttempts[player.uniqueId]
            if (playerAttemptTime != null) {
                val elapsed = now - playerAttemptTime
                if (elapsed in 5001..<30000) {
                    // Allow
                    return
                }
            } else {
                blockAttempts[player.uniqueId] = now
            }
        } else {
            blockAttempts = HashMap<UUID?, Long?>()
            blockAttempts[player.uniqueId] = now
            lootBreakAttempts[block] = blockAttempts
        }

        langBreakLootBlockPrevented!!.send(player)
        event.isCancelled = true
    }
}
