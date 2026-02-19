package org.oddlama.vane.core.item;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.item.api.CustomItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.StorageUtil;

// TODO: what about inventory based item repair?

public class DurabilityManager extends Listener<Core> {

    public static final NamespacedKey ITEM_DURABILITY_MAX = StorageUtil.namespacedKey("vane", "durability.max");
    public static final NamespacedKey ITEM_DURABILITY_DAMAGE = StorageUtil.namespacedKey("vane", "durability.damage");

    private static final NamespacedKey SENTINEL = StorageUtil.namespacedKey("vane", "durability_override_lore");

    public DurabilityManager(final Context<Core> context) {
        super(context);
    }

    /** Returns true if the given component is associated to our custom durability. */
    private static boolean isDurabilityLore(final Component component) {
        return ItemUtil.hasSentinel(component, SENTINEL);
    }

    /** Removes associated lore from an item. */
    private static void removeLore(final ItemStack itemStack) {
        final var lore = itemStack.lore();
        if (lore != null) {
            lore.removeIf(DurabilityManager::isDurabilityLore);
            if (lore.size() > 0) {
                itemStack.lore(lore);
            } else {
                itemStack.lore(null);
            }
        }
    }

    /**
     * Sets the item's damage regarding our custom durability. The durability will get clamped to
     * plausible values. Damage values >= max will result in item breakage. The maximum value will
     * be taken from the item tag if it exists.
     */
    private static void setDamageAndUpdateItem(
        final CustomItem customItem,
        final ItemStack itemStack,
        int damage
    ) {
        // Honor unbreakable flag
        final var roMeta = itemStack.getItemMeta();
        if (roMeta.isUnbreakable()) {
            damage = 0;
        }
        setDamageAndMaxDamage(customItem, itemStack, damage);
    }

    /**
     * Initializes damage on the item, or removes them if custom durability is disabled for the
     * given custom item.
     */
    public static boolean initializeOrUpdateMax(final CustomItem customItem, final ItemStack itemStack) {
        // Remember damage if set.
        var oldDamage = itemStack
            .getItemMeta()
            .getPersistentDataContainer()
            .getOrDefault(ITEM_DURABILITY_DAMAGE, PersistentDataType.INTEGER, -1);

        // First, remove all components.
        itemStack.editMeta(meta -> {
            final var data = meta.getPersistentDataContainer();
            data.remove(ITEM_DURABILITY_DAMAGE);
            data.remove(ITEM_DURABILITY_MAX);
        });

        // The item has no durability anymore. Remove leftover lore and return.
        if (customItem.durability() <= 0) {
            removeLore(itemStack);
            return false;
        }

        final int actualDamage;
        if (oldDamage == -1) {
            if (itemStack.getItemMeta() instanceof final Damageable damageMeta) {
                // If there was no old damage value, initialize proportionally by visual damage.
                final var visualMax = itemStack.getType().getMaxDurability();
                final var damagePercentage = (double) damageMeta.getDamage() / visualMax;
                actualDamage = (int) (customItem.durability() * damagePercentage);
            } else {
                // There was no old damage value, but the item has no visual durability.
                // Initialize with max durability.
                actualDamage = 0;
            }
        } else {
            // Keep old damage.
            actualDamage = oldDamage;
        }

        setDamageAndUpdateItem(customItem, itemStack, actualDamage);

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemDamage(final PlayerItemDamageEvent event) {
        final var item = event.getItem();
        final var customItem = getModule().itemRegistry().get(item);

        // Ignore normal items
        if (customItem == null) {
            return;
        }

        updateDamage(customItem, item);
    }

    /** Update existing max damage to match the configuration */
    public static void updateDamage(CustomItem customItem, ItemStack itemStack) {
        if (!(itemStack.getItemMeta() instanceof Damageable meta)) return; // everything should be damageable now

        boolean updated = false;
        PersistentDataContainer data = meta.getPersistentDataContainer();

        final int newMaxDamage = customItem.durability() == 0
            ? itemStack.getType().getMaxDurability()
            : customItem.durability();

        int oldDamage;
        int oldMaxDamage;
        // if the item has damage in their data, get the value and remove it from PDC
        if (data.has(ITEM_DURABILITY_DAMAGE) && data.has(ITEM_DURABILITY_MAX)) {
            oldDamage = data.get(ITEM_DURABILITY_DAMAGE, PersistentDataType.INTEGER);
            oldMaxDamage = data.get(ITEM_DURABILITY_MAX, PersistentDataType.INTEGER);
            updated = true;
        } else {
            oldDamage = meta.hasDamage() ? meta.getDamage() : 0;
            oldMaxDamage = meta.hasMaxDamage() ? meta.getMaxDamage() : itemStack.getType().getMaxDurability();
        }

        itemStack.editMeta(Damageable.class, imeta -> {
            PersistentDataContainer idata = imeta.getPersistentDataContainer();
            idata.remove(ITEM_DURABILITY_DAMAGE);
            idata.remove(ITEM_DURABILITY_MAX);
        });

        removeLore(itemStack);

        if (!updated) updated = oldMaxDamage != newMaxDamage; // only update if there was old data or a different
        // max
        // durability
        if (!updated) return; // and do nothing if nothing changed
        final int newDamage = scaleDamage(oldDamage, oldMaxDamage, newMaxDamage);
        setDamageAndMaxDamage(customItem, itemStack, newDamage);
    }

    public static int scaleDamage(int oldDamage, int oldMaxDamage, int newMaxDamage) {
        return oldMaxDamage == newMaxDamage
            ? oldDamage
            : (int) (newMaxDamage * ((float) oldDamage / (float) oldMaxDamage));
    }

    public static boolean setDamageAndMaxDamage(CustomItem customItem, ItemStack item, int damage) {
        return item.editMeta(Damageable.class, meta -> {
            if (customItem.durability() != 0) {
                meta.setMaxDamage(customItem.durability());
            } else {
                meta.setMaxDamage((int) item.getType().getMaxDurability());
            }

            meta.setDamage(damage);
        });
    }
}
