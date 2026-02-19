package org.oddlama.vane.portals;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class PortalBlockProtector extends Listener<Portals> {

    public PortalBlockProtector(Context<Portals> context) {
        super(context);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        // Prevent breaking of portal blocks
        if (getModule().isPortalBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        // Prevent (re-)placing of portal blocks
        if (getModule().isPortalBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        // Prevent explosions from removing portal blocks
        event.blockList().removeIf(block -> getModule().isPortalBlock(block));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        // Prevent entities from changing portal blocks
        if (getModule().isPortalBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
        // Prevent pistons from moving portal blocks
        for (final var block : event.getBlocks()) {
            if (getModule().isPortalBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonRetract(final BlockPistonRetractEvent event) {
        // Prevent pistons from moving portal blocks
        for (final var block : event.getBlocks()) {
            if (getModule().isPortalBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
