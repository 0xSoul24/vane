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

/** Protects portal blocks from direct world interactions. */
class PortalBlockProtector(context: Context<Portals?>?) : Listener<Portals?>(context) {
    /** Cancels breaking of registered portal blocks. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val module = module ?: return
        // Prevent breaking of portal blocks
        if (module.isPortalBlock(event.block)) {
            event.isCancelled = true
        }
    }

    /** Cancels placing over registered portal blocks. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val module = module ?: return
        // Prevent (re-)placing of portal blocks
        if (module.isPortalBlock(event.block)) {
            event.isCancelled = true
        }
    }

    /** Removes portal blocks from explosion destruction lists. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        val module = module ?: return
        // Prevent explosions from removing portal blocks
        event.blockList().removeIf(module::isPortalBlock)
    }

    /** Cancels entity-driven block changes on portal blocks. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        val module = module ?: return
        // Prevent entities from changing portal blocks
        if (module.isPortalBlock(event.block)) {
            event.isCancelled = true
        }
    }

    /** Cancels piston extension when portal blocks would be moved. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockPistonExtend(event: BlockPistonExtendEvent) {
        val module = module ?: return
        // Prevent pistons from moving portal blocks
        if (event.blocks.any(module::isPortalBlock)) {
            event.isCancelled = true
        }
    }

    /** Cancels piston retraction when portal blocks would be moved. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockPistonRetract(event: BlockPistonRetractEvent) {
        val module = module ?: return
        // Prevent pistons from moving portal blocks
        if (event.blocks.any(module::isPortalBlock)) {
            event.isCancelled = true
        }
    }
}
