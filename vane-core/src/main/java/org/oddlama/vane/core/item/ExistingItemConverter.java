package org.oddlama.vane.core.item;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.item.api.CustomItem;
import org.oddlama.vane.core.module.Context;

public class ExistingItemConverter extends Listener<Core> {
    public ExistingItemConverter(final Context<Core> context) {
        super(context.namespace("existing_item_converter"));
    }

    private CustomItem fromOldItem(final ItemStack itemStack) {
        final var modelDataList = itemStack.getItemMeta().getCustomModelDataComponent().getFloats();
        if (modelDataList.isEmpty() || modelDataList.getFirst() == null) {
            return null;
        }

        // If lookups fail, we return null and nothing will be done.
        String newItemKey = switch (modelDataList.getFirst().intValue()) {
            case 7758190 -> "vane_trifles:wooden_sickle";
            case 7758191 -> "vane_trifles:stone_sickle";
            case 7758192 -> "vane_trifles:iron_sickle";
            case 7758193 -> "vane_trifles:golden_sickle";
            case 7758194 -> "vane_trifles:diamond_sickle";
            case 7758195 -> "vane_trifles:netherite_sickle";
            case 7758254,7758255,7758256,7758257,7758258,7758259 -> "vane_trifles:file";
            case 7758318 -> "vane_trifles:empty_xp_bottle";
            case 7758382 -> "vane_trifles:small_xp_bottle";
            case 7758383 -> "vane_trifles:medium_xp_bottle";
            case 7758384 -> "vane_trifles:large_xp_bottle";
            case 7758446 -> "vane_trifles:home_scroll";
            case 7758510 -> "vane_trifles:unstable_scroll";
            case 7758574 -> "vane_trifles:reinforced_elytra";
            case 7823726 -> "vane_enchantments:ancient_tome";
            case 7823727 -> "vane_enchantments:enchanted_ancient_tome";
            case 7823790 -> "vane_enchantments:ancient_tome_of_knowledge";
            case 7823791 -> "vane_enchantments:enchanted_ancient_tome_of_knowledge";
            case 7823854 -> "vane_enchantments:ancient_tome_of_the_gods";
            case 7823855 -> "vane_enchantments:enchanted_ancient_tome_of_the_gods";
            default -> null;
        };

        if (newItemKey == null) {
            return null;
        }
        return getModule().itemRegistry().get(NamespacedKey.fromString(newItemKey));
    }

    private void processInventory(@NotNull Inventory inventory) {
        final var contents = inventory.getContents();
        int changed = 0;

        for (int i = 0; i < contents.length; ++i) {
            final var is = contents[i];
            if (is == null || !is.hasItemMeta()) {
                continue;
            }

            final var customItem = getModule().itemRegistry().get(is);
            if (customItem == null) {
                // Determine if the item stack should be converted to a custom item from a legacy
                // definition
                final var convertToCustomItem = fromOldItem(is);
                if (convertToCustomItem == null) {
                    continue;
                }

                contents[i] = convertToCustomItem.convertExistingStack(is);
                contents[i].editMeta(meta -> meta.itemName(convertToCustomItem.displayName()));
                getModule().enchantmentManager.updateEnchantedItem(contents[i]);
                getModule().log.info("Converted legacy item to " + convertToCustomItem.key());
                ++changed;
                continue;
            }

            // Remove obsolete custom items
            if (getModule().itemRegistry().shouldRemove(customItem.key())) {
                contents[i] = null;
                getModule().log.info("Removed obsolete item " + customItem.key());
                ++changed;
                continue;
            }

            // Update custom items to a new version, or if another detectable property changed.
            final var keyAndVersion = CustomItemHelper.customItemTagsFromItemStack(is);
            final var meta = is.getItemMeta();
            final var modelDataList = meta.getCustomModelDataComponent().getFloats();
            final Integer modelDataInt = (!modelDataList.isEmpty() && modelDataList.getFirst() != null)
                ? modelDataList.getFirst().intValue()
                : null;

            if (
                modelDataInt == null ||
                modelDataInt != customItem.customModelData() ||
                is.getType() != customItem.baseMaterial() ||
                keyAndVersion.getRight() != customItem.version()) {
                // Also includes durability max update.
                contents[i] = customItem.convertExistingStack(is);
                getModule().log.info("Updated item " + customItem.key());
                ++changed;
                continue;
            }

            // Update maximum durability on existing items if changed.
            Damageable damageableMeta = (Damageable) contents[i].getItemMeta();
            int maxDamage = damageableMeta.hasMaxDamage()
                ? damageableMeta.getMaxDamage()
                : contents[i].getType().getMaxDurability();
            int correctMaxDamage = customItem.durability() == 0
                ? contents[i].getType().getMaxDurability()
                : customItem.durability();
            if (
                maxDamage != correctMaxDamage ||
                meta.getPersistentDataContainer().has(DurabilityManager.ITEM_DURABILITY_DAMAGE)) {
                getModule().log.info("Updated item durability " + customItem.key());
                DurabilityManager.updateDamage(customItem, contents[i]);
                ++changed;
                continue;
            }
        }

        if (changed > 0) {
            inventory.setContents(contents);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        processInventory(event.getPlayer().getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(final InventoryOpenEvent event) {
        // Catches enderchests, and inventories by other plugins
        processInventory(event.getInventory());
    }
}
