package org.oddlama.vane.trifles

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.MaterialUtil
import org.oddlama.vane.util.PlayerUtil

class HarvestListener(context: Context<Trifles?>) : Listener<Trifles?>(
    context.group(
        "BetterHarvesting",
        "Enables better harvesting. Right clicking on grown crops with bare hands will then harvest the plant and also replant it."
    )
) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerHarvest(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.hand != EquipmentSlot.HAND || event.action != Action.RIGHT_CLICK_BLOCK
        ) {
            return
        }

        // Only harvest when right-clicking some plant type
        val type = event.clickedBlock!!.type
        if (!MaterialUtil.isSeededPlant(type)) {
            return
        }

        val player = event.getPlayer()
        if (PlayerUtil.harvestPlant(player, event.clickedBlock!!)) {
            PlayerUtil.swingArm(player, event.hand!!)
        }
    }
}
