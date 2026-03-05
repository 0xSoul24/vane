package org.oddlama.vane.portals

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.BlockFace
import org.bukkit.block.data.FaceAttachable.AttachedFace
import org.bukkit.block.data.type.Repeater
import org.bukkit.block.data.type.Switch
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.portals.portal.Portal
import org.oddlama.vane.portals.portal.PortalBlock
import org.oddlama.vane.portals.portal.PortalBlockLookup
import org.oddlama.vane.util.PlayerUtil

class PortalActivator(context: Context<Portals?>?) : Listener<Portals?>(context) {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    fun onPlayerInteractConsole(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        if (event.useInteractedBlock() == Event.Result.DENY) {
            return
        }

        // Abort if the table is not a console
        val block = event.clickedBlock ?: return
        val portalBlock: PortalBlockLookup? = module!!.portalBlockFor(block)
        if (portalBlock == null || portalBlock.type() != PortalBlock.Type.CONSOLE) {
            return
        }

        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)

        val player = event.getPlayer()
        val portal: Portal = module!!.portalFor(portalBlock)
        if (portal.openConsole(module!!, player, block)) {
            PlayerUtil.swingArm(player, event.hand!!)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteractSwitch(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        if (event.useInteractedBlock() == Event.Result.DENY) {
            return
        }

        val block = event.clickedBlock ?: return
        val allowDisable: Boolean = if (block.type == Material.LEVER) {
            true
        } else if (Tag.BUTTONS.isTagged(block.type)) {
            false
        } else {
            return
        }

        // Get base block the switch is attached to
        val bswitch = block.blockData as Switch
        val attachedFace: BlockFace = when (bswitch.attachedFace) {
            AttachedFace.WALL -> bswitch.facing.getOppositeFace()
            AttachedFace.CEILING -> BlockFace.UP
            AttachedFace.FLOOR -> BlockFace.DOWN
        }

        // Find controlled portal
        val base = block.getRelative(attachedFace)
        val portal = module!!.controlledPortal(base) ?: return

        val player = event.getPlayer()
        val active: Boolean = module!!.isActivated(portal)
        if (bswitch.isPowered && allowDisable) {
            if (!active) {
                return
            }

            // Switch is being switched off → deactivate
            if (!portal.deactivate(module!!, player)) {
                event.setUseInteractedBlock(Event.Result.DENY)
                event.setUseItemInHand(Event.Result.DENY)
            }
        } else {
            if (active) {
                return
            }

            // Switch is being switched on → activate
            if (!portal.activate(module!!, player)) {
                event.setUseInteractedBlock(Event.Result.DENY)
                event.setUseItemInHand(Event.Result.DENY)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockRedstone(event: BlockRedstoneEvent) {
        // Only on rising edge.
        if (event.oldCurrent != 0 || event.newCurrent == 0) {
            return
        }

        // Only repeaters
        val block = event.getBlock()
        if (block.type != Material.REPEATER) {
            return
        }

        // Get the block it's pointing towards. (Opposite of block's facing for repeaters)
        val repeater = block.blockData as Repeater
        val intoBlock = block.getRelative(repeater.facing.getOppositeFace())

        // Find controlled portal
        val portal = module!!.portalFor(intoBlock) ?: return

        portal.activate(module!!, null)
    }
}
