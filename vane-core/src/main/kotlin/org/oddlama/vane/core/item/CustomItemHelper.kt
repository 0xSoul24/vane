@file:Suppress("EXPERIMENTAL_API_USAGE")
package org.oddlama.vane.core.item

import org.apache.commons.lang3.tuple.Pair
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.util.StorageUtil.namespacedKey

/**
 * Utilities for creating, tagging, and converting custom item stacks.
 */
object CustomItemHelper {
    /**
     * Persistent-data key used to store a custom item identifier.
     */
    val CUSTOM_ITEM_IDENTIFIER: NamespacedKey = namespacedKey(
        "vane",
        "custom_item_identifier"
    )

    /**
     * Persistent-data key used to store the custom item version.
     */
    val CUSTOM_ITEM_VERSION: NamespacedKey = namespacedKey("vane", "custom_item_version")

    /**
     * Updates stack metadata for a custom item and delegates to item-specific updates.
     */
    fun updateItemStack(customItem: CustomItem, itemStack: ItemStack): ItemStack {
        itemStack.editMeta { meta: ItemMeta ->
            val data = meta.persistentDataContainer
            data.set(CUSTOM_ITEM_IDENTIFIER, PersistentDataType.STRING, customItem.key().toString())
            data.set(CUSTOM_ITEM_VERSION, PersistentDataType.INTEGER, customItem.version())
            val customModelDataComponent = meta.customModelDataComponent
            customModelDataComponent.floats = listOf(customItem.customModelData().toFloat())
            meta.setCustomModelDataComponent(customModelDataComponent)
        }

        DurabilityManager.initializeOrUpdateMax(customItem, itemStack)
        return customItem.updateItemStack(itemStack)
    }

    /**
     * Reads custom-item identifier and version tags from an item stack.
     */
    @JvmStatic
    fun customItemTagsFromItemStack(itemStack: ItemStack?): Pair<NamespacedKey?, Int?>? {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null
        }

        val data = itemStack.itemMeta.persistentDataContainer
        val key: String? = data.get(CUSTOM_ITEM_IDENTIFIER, PersistentDataType.STRING)
        val version: Int? = data.get(CUSTOM_ITEM_VERSION, PersistentDataType.INTEGER)
        if (key == null || version == null) {
            return null
        }

        val parts: Array<String?> = key.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        check(parts.size == 2) { "Invalid namespaced key '$key'" }
        return Pair.of<NamespacedKey?, Int?>(namespacedKey(parts[0]!!, parts[1]!!), version)
    }

    /**
     * Creates a single-item stack for a custom item key.
     */
    @JvmStatic
    fun newStack(customItemKey: String): ItemStack {
        return newStack(customItemKey, 1)
    }

    /**
     * Creates a stack for a custom item key with a specific amount.
     */
    fun newStack(customItemKey: String, amount: Int): ItemStack {
        val registry = Core.instance()?.itemRegistry()
            ?: throw IllegalStateException("CustomItemRegistry is not initialized")
        val key = NamespacedKey.fromString(customItemKey)
            ?: throw IllegalArgumentException("Invalid namespaced key: $customItemKey")
        val ci = registry.get(key)
            ?: throw IllegalArgumentException("Unknown custom item: $customItemKey")
        return newStack(ci, amount)
    }

    /**
     * Creates a single-item stack for a custom item.
     */
    @JvmStatic
    fun newStack(customItem: CustomItem): ItemStack {
        return newStack(customItem, 1)
    }

    /**
     * Creates a stack for a custom item with a specific amount.
     */
    @JvmStatic
    fun newStack(customItem: CustomItem, amount: Int): ItemStack {
        val itemStack = ItemStack(customItem.baseMaterial(), amount)
        itemStack.editMeta(ItemMeta::class.java) { meta -> meta.itemName(customItem.displayName()) }
        return updateItemStack(customItem, itemStack)
    }

    /**
     * Converts an existing stack to the target custom item while preserving stack metadata.
     */
    @JvmStatic
    fun convertExistingStack(customItem: CustomItem, itemStack: ItemStack): ItemStack {
        var itemStack = itemStack
        itemStack = itemStack.clone().withType(customItem.baseMaterial())
        return updateItemStack(customItem, itemStack)
    }
}
