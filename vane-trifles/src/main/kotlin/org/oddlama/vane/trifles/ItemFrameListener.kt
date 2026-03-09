package org.oddlama.vane.trifles

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context

class ItemFrameListener(context: Context<Trifles?>) : Listener<Trifles?>(
    context.group(
        "InvisibleItemFrame",
        "Right clicking on an item frame with shears equipped will make it disappear."
    )
) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: PlayerInteractEntityEvent) {
        val player = event.getPlayer()
        val entity = event.rightClicked
        val isHoldingShears = player.inventory.itemInMainHand.type == Material.SHEARS
        val entityIsItemFrame = entity.type == EntityType.ITEM_FRAME

        if (isHoldingShears && entityIsItemFrame) {
            event.isCancelled = true
            entity.isInvisible = !entity.isInvisible
        }
    }
}
