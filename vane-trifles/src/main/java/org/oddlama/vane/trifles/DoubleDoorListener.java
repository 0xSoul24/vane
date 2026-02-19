package org.oddlama.vane.trifles;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class DoubleDoorListener extends Listener<Trifles> {

    public DoubleDoorListener(Context<Trifles> context) {
        super(
            context.group(
                "DoubleDoor",
                "Enable updating of double doors automatically when one of the doors is changed."
            )
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.hasBlock() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getPlayer().isSneaking()) {
				return;
			}
            handleDoubleDoor(event.getClickedBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        final var block = event.getBlock();
        handleDoubleDoor(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        var now = event.getNewCurrent();
        var old = event.getOldCurrent();
        if (now != old && (now == 0 || old == 0)) {
            // only on / off changes
            handleDoubleDoor(event.getBlock());
        }
    }

    public void handleDoubleDoor(final Block block) {
        final var first = SingleDoor.createDoorFromBlock(block);
        if (first == null) {
            return;
        }
        final var second = first.getSecondDoor();
        if (second == null) {
            return;
        }

        // Update second door state directly after the event (delay 0)
        scheduleNextTick(() -> {
            // Make sure to include changes from last tick
            if (!first.updateCachedState() || !second.updateCachedState()) {
                return;
            }

            second.setOpen(first.isOpen());
        });
    }
}
