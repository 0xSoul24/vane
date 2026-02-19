package org.oddlama.vane.core.item;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.item.api.CustomItem;
import org.oddlama.vane.core.item.api.CustomModelDataRegistry;

public class CustomItemRegistry implements org.oddlama.vane.core.item.api.CustomItemRegistry {

    private final HashMap<NamespacedKey, CustomItem> items = new HashMap<>();
    private final HashSet<NamespacedKey> itemsToRemove = new HashSet<>();
    private CustomModelDataRegistry modelDataRegistry;

    public CustomItemRegistry() {
        this.modelDataRegistry = Core.instance().modelDataRegistry();
    }

    @Override
    public @Nullable boolean has(final NamespacedKey resourceKey) {
        return items.containsKey(resourceKey);
    }

    @Override
    public @Nullable Collection<CustomItem> all() {
        return items.values();
    }

    @Override
    public @Nullable CustomItem get(final NamespacedKey resourceKey) {
        return items.get(resourceKey);
    }

    @Override
    public @Nullable CustomItem get(@Nullable final ItemStack itemStack) {
        final var keyAndVersion = CustomItemHelper.customItemTagsFromItemStack(itemStack);
        if (keyAndVersion == null) {
            return null;
        }

        return get(keyAndVersion.getLeft());
    }

    @Override
    public void register(final CustomItem customItem) {
        modelDataRegistry.reserveSingle(customItem.key(), customItem.customModelData());
        if (has(customItem.key())) {
            throw new IllegalArgumentException(
                "A custom item with the same key '" + customItem.key() + "' has already been registered"
            );
        }
        items.put(customItem.key(), customItem);
    }

    @Override
    public void removePermanently(final NamespacedKey key) {
        itemsToRemove.add(key);
    }

    @Override
    public boolean shouldRemove(NamespacedKey key) {
        return itemsToRemove.contains(key);
    }

    @Override
    public CustomModelDataRegistry dataRegistry() {
        return modelDataRegistry;
    }
}
