package org.oddlama.vane.regions.event;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.Regions;

public class RegionSelectionListener extends Listener<Regions> {

    @LangMessage
    public TranslatedMessage langSelectPrimaryBlock;

    @LangMessage
    public TranslatedMessage langSelectSecondaryBlock;

    public RegionSelectionListener(Context<Regions> context) {
        super(context);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        // Require the main hand event
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // Require empty hand
        if (event.getItem() != null) {
            return;
        }

        final var player = event.getPlayer();
        final var selection = getModule().getRegionSelection(player);
        if (selection == null) {
            return;
        }

        if (
            player.getEquipment().getItemInMainHand().getType() != Material.AIR ||
            player.getEquipment().getItemInOffHand().getType() != Material.AIR
        ) {
            return;
        }

        final var block = event.getClickedBlock();
        switch (event.getAction()) {
            default:
                return;
            case LEFT_CLICK_BLOCK:
                selection.primary = block;
                langSelectPrimaryBlock.send(player, "§b" + block.getX(), "§b" + block.getY(), "§b" + block.getZ());
                break;
            case RIGHT_CLICK_BLOCK:
                selection.secondary = block;
                langSelectSecondaryBlock.send(player, "§b" + block.getX(), "§b" + block.getY(), "§b" + block.getZ());
                break;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }
}
