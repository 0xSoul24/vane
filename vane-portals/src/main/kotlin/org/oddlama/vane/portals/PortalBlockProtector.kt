package org.oddlama.vane.portals

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context

class PortalBlockProtector(context: Context<Portals?>?) : Listener<Portals?>(context) {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        // Prevent breaking of portal blocks
        if (module!!.isPortalBlock(event.block)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        // Prevent (re-)placing of portal blocks
        if (module!!.isPortalBlock(event.block)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        // Prevent explosions from removing portal blocks
        event.blockList().removeIf { block -> module!!.isPortalBlock(block) }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        // Prevent entities from changing portal blocks
        if (module!!.isPortalBlock(event.block)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockPistonExtend(event: BlockPistonExtendEvent) {
        // Prevent pistons from moving portal blocks
        for (block in event.blocks) {
            if (module!!.isPortalBlock(block)) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockPistonRetract(event: BlockPistonRetractEvent) {
        // Prevent pistons from moving portal blocks
        for (block in event.blocks) {
            if (module!!.isPortalBlock(block)) {
                event.isCancelled = true
                return
            }
        }
    }
}
