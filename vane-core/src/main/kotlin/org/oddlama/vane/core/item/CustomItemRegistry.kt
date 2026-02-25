package org.oddlama.vane.core.item

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.item.api.CustomItemRegistry
import org.oddlama.vane.core.item.api.CustomModelDataRegistry

class CustomItemRegistry : CustomItemRegistry {
    private val items = HashMap<NamespacedKey?, CustomItem?>()
    private val itemsToRemove = HashSet<NamespacedKey?>()
    private val modelDataRegistry: CustomModelDataRegistry? = Core.instance()?.modelDataRegistry()

    override fun has(resourceKey: NamespacedKey?): Boolean {
        return items.containsKey(resourceKey)
    }

    override fun all(): MutableCollection<CustomItem?> {
        return items.values
    }

    override fun get(resourceKey: NamespacedKey?): CustomItem? {
        return items[resourceKey]
    }

    override fun get(itemStack: ItemStack?): CustomItem? {
        val keyAndVersion = CustomItemHelper.customItemTagsFromItemStack(itemStack) ?: return null

        return get(keyAndVersion.getLeft())
    }

    // Match interface: accept nullable CustomItem and handle null safely
    override fun register(customItem: CustomItem?) {
        if (customItem == null) return
        modelDataRegistry?.reserveSingle(customItem.key(), customItem.customModelData())
        require(!has(customItem.key())) { "A custom item with the same key '" + customItem.key() + "' has already been registered" }
        items[customItem.key()] = customItem
    }

    override fun removePermanently(key: NamespacedKey?) {
        itemsToRemove.add(key)
    }

    override fun shouldRemove(key: NamespacedKey?): Boolean {
        return itemsToRemove.contains(key)
    }

    override fun dataRegistry(): CustomModelDataRegistry? {
        return modelDataRegistry
    }
}
