package org.oddlama.vane.trifles

import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.SingleDoor.Companion.createDoorFromBlock

class DoubleDoorListener(context: Context<Trifles?>) : Listener<Trifles?>(
    context.group(
        "DoubleDoor",
        "Enable updating of double doors automatically when one of the doors is changed."
    )
) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hasBlock() && event.action == Action.RIGHT_CLICK_BLOCK) {
            if (event.getPlayer().isSneaking) {
                return
            }
            handleDoubleDoor(event.clickedBlock!!)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityInteract(event: EntityInteractEvent) {
        val block = event.getBlock()
        handleDoubleDoor(block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockRedstone(event: BlockRedstoneEvent) {
        val now = event.newCurrent
        val old = event.oldCurrent
        if (now != old && (now == 0 || old == 0)) {
            // only on / off changes
            handleDoubleDoor(event.getBlock())
        }
    }

    fun handleDoubleDoor(block: Block) {
        val first = createDoorFromBlock(block) ?: return
        val second = first.secondDoor ?: return

        // Update second door state directly after the event (delay 0)
        scheduleNextTick {
            // Make sure to include changes from last tick
            if (!first.updateCachedState() || !second.updateCachedState()) {
                return@scheduleNextTick
            }
            second.isOpen = first.isOpen
        }
    }
}
