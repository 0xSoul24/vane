package org.oddlama.vane.core.item;

import static org.oddlama.vane.util.MaterialUtil.isTillable;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.SmithingRecipe;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.item.api.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;

// TODO recipe book click event
public class VanillaFunctionalityInhibitor extends Listener<Core> {

    public VanillaFunctionalityInhibitor(Context<Core> context) {
        super(context);
    }

    private boolean inhibit(CustomItem customItem, InhibitBehavior behavior) {
        return customItem != null && customItem.enabled() && customItem.inhibitedBehaviors().contains(behavior);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPathfind(final EntityTargetEvent event) {
        if (event.getReason() != EntityTargetEvent.TargetReason.TEMPT) {
            return;
        }

        if (event.getTarget() instanceof Player player) {
            final var customItemMain = getModule().itemRegistry().get(player.getInventory().getItemInMainHand());
            final var customItemOff = getModule().itemRegistry().get(player.getInventory().getItemInOffHand());

            if (inhibit(customItemMain, InhibitBehavior.TEMPT) || inhibit(customItemOff, InhibitBehavior.TEMPT)) {
                event.setCancelled(true);
            }
        }
    }

    // Prevent custom hoe items from tilling blocks
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerHoeRightClickBlock(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Only when clicking a tillable block
        if (!isTillable(event.getClickedBlock().getType())) {
            return;
        }

        final var player = event.getPlayer();
        final var item = player.getEquipment().getItem(event.getHand());
        if (inhibit(getModule().itemRegistry().get(item), InhibitBehavior.HOE_TILL)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrepareItemCraft(final PrepareItemCraftEvent event) {
        final var recipe = event.getRecipe();
        if (!(recipe instanceof Keyed keyed)) {
            return;
        }

        // Only consider canceling minecraft's recipes
        if (!keyed.getKey().getNamespace().equals("minecraft")) {
            return;
        }

        for (final var item : event.getInventory().getMatrix()) {
            if (inhibit(getModule().itemRegistry().get(item), InhibitBehavior.USE_IN_VANILLA_RECIPE)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    // Prevent custom items from being used in smithing by default. They have to override this event
    // to allow it.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrepareSmithing(final PrepareSmithingEvent event) {
        final var item = event.getInventory().getInputEquipment();
        final var recipe = event.getInventory().getRecipe();
        if (!(recipe instanceof Keyed keyed)) {
            return;
        }

        // Only consider canceling Minecraft's recipes
        if (!keyed.getKey().getNamespace().equals("minecraft")) {
            return;
        }

        if (inhibit(getModule().itemRegistry().get(item), InhibitBehavior.USE_IN_VANILLA_RECIPE)) {
            event.getInventory().setResult(null);
        }
    }

    // If the result of a smithing recipe is a custom item, copy and merge input NBT data.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPrepareSmithingCopyNbt(final PrepareSmithingEvent event) {
        var result = event.getResult();
        final var recipe = event.getInventory().getRecipe();
        if (result == null || !(recipe instanceof SmithingRecipe smithingRecipe) || !smithingRecipe.willCopyNbt()) {
            return;
        }

        // Actually use a recipe result, as copynbt has already modified the result
        result = recipe.getResult();
        final var customItemResult = getModule().itemRegistry().get(result);
        if (customItemResult == null) {
            return;
        }

        final var input = event.getInventory().getInputEquipment();
        final var inputComponents = CraftItemStack.asNMSCopy(input).getComponents();
        final var nmsResult = CraftItemStack.asNMSCopy(result);
        nmsResult.applyComponents(inputComponents);

        event.setResult(customItemResult.convertExistingStack(CraftItemStack.asCraftMirror(nmsResult)));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrepareAnvil(final PrepareAnvilEvent event) {
        final var a = event.getInventory().getFirstItem();
        final var b = event.getInventory().getSecondItem();

        // Always prevent custom item repair with the custom item base material
        // if it is not also a matching custom item.
        // TODO: what about inventory based item repair?
        if (a != null && b != null && a.getType() == b.getType()) {
            // Disable the result unless a and b are instances of the same custom item.
            final var customItemA = getModule().itemRegistry().get(a);
            final var customItemB = getModule().itemRegistry().get(b);
            if (customItemA != null && customItemA != customItemB) {
                event.setResult(null);
                return;
            }
        }

        final var r = event.getInventory().getResult();
        if (r != null) {
            final var customItemR = getModule().itemRegistry().get(r);
            final boolean[] didEdit = new boolean[] { true };
            r.editMeta(meta -> {
                if (a != null && inhibit(customItemR, InhibitBehavior.NEW_ENCHANTS)) {
                    for (final var ench : r.getEnchantments().keySet()) {
                        if (!a.getEnchantments().containsKey(ench)) {
                            meta.removeEnchant(ench);
                            didEdit[0] = true;
                        }
                    }
                }

                if (inhibit(customItemR, InhibitBehavior.MEND)) {
                    meta.removeEnchant(Enchantment.MENDING);
                    didEdit[0] = true;
                }
            });

            if (didEdit[0]) {
                event.setResult(r);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemMend(final PlayerItemMendEvent event) {
        final var item = event.getItem();
        final var customItem = getModule().itemRegistry().get(item);

        // No repairing for mending inhibited items.
        if (inhibit(customItem, InhibitBehavior.MEND)) {
            event.setCancelled(true);
        }
    }

    // Prevent netherite items from burning, as they are made of netherite
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemBurn(final EntityDamageEvent event) {
        // Only burn damage on dropped items
        if (event.getEntity().getType() != EntityType.ITEM) {
            return;
        }

        switch (event.getCause()) {
            default:
                return;
            case FIRE:
            case FIRE_TICK:
            case LAVA:
                break;
        }

        final var entity = event.getEntity();
        if (!(entity instanceof Item)) {
            return;
        }

        final var item = ((Item) entity).getItemStack();
        if (inhibit(getModule().itemRegistry().get(item), InhibitBehavior.ITEM_BURN)) {
            event.setCancelled(true);
        }
    }

    // Deny off-hand usage if the main hand is a custom item that inhibits off-hand usage.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerRightClick(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.OFF_HAND) {
            return;
        }

        final var player = event.getPlayer();
        final var mainItem = player.getEquipment().getItem(EquipmentSlot.HAND);
        final var mainCustomItem = getModule().itemRegistry().get(mainItem);
        if (inhibit(mainCustomItem, InhibitBehavior.USE_OFFHAND)) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (event.getBlock().getType() != Material.DISPENSER) {
            return;
        }

        final var customItem = getModule().itemRegistry().get(event.getItem());
        if (inhibit(customItem, InhibitBehavior.DISPENSE)) {
            event.setCancelled(true);
        }
    }
}
