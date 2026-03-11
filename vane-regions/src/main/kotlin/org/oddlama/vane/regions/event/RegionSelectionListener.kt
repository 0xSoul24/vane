package org.oddlama.vane.regions.event

import org.bukkit.Material
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.regions.region.RegionSelection

class RegionSelectionListener(context: Context<Regions?>?) : Listener<Regions?>(context) {
    @LangMessage
    var langSelectPrimaryBlock: TranslatedMessage? = null

    @LangMessage
    var langSelectSecondaryBlock: TranslatedMessage? = null

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Require the main hand event
        if (event.hand != EquipmentSlot.HAND) {
            return
        }

        // Require empty hand
        if (event.item != null) {
            return
        }

        val player = event.player
        if (!module!!.isSelectingRegion(player)) return
        val selection: RegionSelection = module!!.getRegionSelection(player)

        if (player.equipment.itemInMainHand.type != Material.AIR ||
            player.equipment.itemInOffHand.type != Material.AIR
        ) {
            return
        }

        val block = event.clickedBlock
        when (event.action) {
            Action.LEFT_CLICK_BLOCK -> {
                selection.primary = block
                langSelectPrimaryBlock!!.send(
                    player,
                    "§b" + block!!.x,
                    "§b" + block.y,
                    "§b" + block.z
                )
            }

            Action.RIGHT_CLICK_BLOCK -> {
                selection.secondary = block
                langSelectSecondaryBlock!!.send(
                    player,
                    "§b" + block!!.x,
                    "§b" + block.y,
                    "§b" + block.z
                )
            }

            else -> return
        }

        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)
    }
}
