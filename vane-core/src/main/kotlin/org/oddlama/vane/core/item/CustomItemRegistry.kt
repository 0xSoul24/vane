package org.oddlama.vane.core.item

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.item.api.CustomItemRegistry
import org.oddlama.vane.core.item.api.CustomModelDataRegistry

class CustomItemRegistry : CustomItemRegistry {
    private val items = mutableMapOf<NamespacedKey, CustomItem>()
    private val itemsToRemove = mutableSetOf<NamespacedKey>()
    private val modelDataRegistry: CustomModelDataRegistry? = Core.instance()?.modelDataRegistry()

    override fun has(resourceKey: NamespacedKey): Boolean = resourceKey in items

    override fun all(): Collection<CustomItem> = items.values

    override fun get(resourceKey: NamespacedKey): CustomItem? = items[resourceKey]

    override fun get(itemStack: ItemStack?): CustomItem? {
        val keyAndVersion = CustomItemHelper.customItemTagsFromItemStack(itemStack) ?: return null
        val key = keyAndVersion.getLeft() ?: return null
        return get(key)
    }

    override fun register(customItem: CustomItem) {
        modelDataRegistry?.reserveSingle(customItem.key(), customItem.customModelData())
        require(!has(customItem.key())) {
            "A custom item with the same key '${customItem.key()}' has already been registered"
        }
        items[customItem.key()] = customItem
    }

    override fun removePermanently(key: NamespacedKey) {
        itemsToRemove.add(key)
    }

    override fun shouldRemove(key: NamespacedKey): Boolean = key in itemsToRemove

    override fun dataRegistry(): CustomModelDataRegistry = requireNotNull(modelDataRegistry) {
        "CustomModelDataRegistry is not available"
    }
}
