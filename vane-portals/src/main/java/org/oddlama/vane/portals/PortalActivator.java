package org.oddlama.vane.portals;

import static org.oddlama.vane.util.PlayerUtil.swingArm;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.portals.portal.PortalBlock;

public class PortalActivator extends Listener<Portals> {

    public PortalActivator(Context<Portals> context) {
        super(context);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onPlayerInteractConsole(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }

        // Abort if the table is not a console
        final var block = event.getClickedBlock();
        final var portalBlock = getModule().portalBlockFor(block);
        if (portalBlock == null || portalBlock.type() != PortalBlock.Type.CONSOLE) {
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        final var player = event.getPlayer();
        final var portal = getModule().portalFor(portalBlock);
        if (portal.openConsole(getModule(), player, block)) {
            swingArm(player, event.getHand());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractSwitch(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }

        final var block = event.getClickedBlock();
        final boolean allowDisable;
        if (block.getType() == Material.LEVER) {
            allowDisable = true;
        } else if (Tag.BUTTONS.isTagged(block.getType())) {
            allowDisable = false;
        } else {
            return;
        }

        // Get base block the switch is attached to
        final var bswitch = (Switch) block.getBlockData();
        final BlockFace attachedFace;
        switch (bswitch.getAttachedFace()) {
            default:
            case WALL:
                attachedFace = bswitch.getFacing().getOppositeFace();
                break;
            case CEILING:
                attachedFace = BlockFace.UP;
                break;
            case FLOOR:
                attachedFace = BlockFace.DOWN;
                break;
        }

        // Find controlled portal
        final var base = block.getRelative(attachedFace);
        final var portal = getModule().controlledPortal(base);
        if (portal == null) {
            return;
        }

        final var player = event.getPlayer();
        final var active = getModule().isActivated(portal);
        if (bswitch.isPowered() && allowDisable) {
            if (!active) {
                return;
            }

            // Switch is being switched off → deactivate
            if (!portal.deactivate(getModule(), player)) {
                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);
            }
        } else {
            if (active) {
                return;
            }

            // Switch is being switched on → activate
            if (!portal.activate(getModule(), player)) {
                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockRedstone(final BlockRedstoneEvent event) {
        // Only on rising edge.
        if (event.getOldCurrent() != 0 || event.getNewCurrent() == 0) {
            return;
        }

        // Only repeaters
        final var block = event.getBlock();
        if (block.getType() != Material.REPEATER) {
            return;
        }

        // Get the block it's pointing towards. (Opposite of block's facing for repeaters)
        final var repeater = (Repeater) block.getBlockData();
        final var intoBlock = block.getRelative(repeater.getFacing().getOppositeFace());

        // Find controlled portal
        final var portal = getModule().portalFor(intoBlock);
        if (portal == null) {
            return;
        }

        portal.activate(getModule(), null);
    }
}
