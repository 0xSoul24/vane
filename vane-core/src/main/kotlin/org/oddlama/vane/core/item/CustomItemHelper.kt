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
import java.util.function.Consumer

object CustomItemHelper {
    /** Used in persistent item storage to identify custom items.  */
    val CUSTOM_ITEM_IDENTIFIER: NamespacedKey = namespacedKey(
        "vane",
        "custom_item_identifier"
    )

    /** Used in persistent item storage to store a custom item version.  */
    val CUSTOM_ITEM_VERSION: NamespacedKey = namespacedKey("vane", "custom_item_version")

    /**
     * Internal function. Acts as a dispatcher that updates internal metadata on the provided
     * ItemStack (persistent-data tags, model data, durability) and then calls the
     * CustomItem-specific update method so subclasses can apply any additional changes.
     * This prevents information de-sync in case a subclass forgets to call super.
     */
    fun updateItemStack(customItem: CustomItem, itemStack: ItemStack): ItemStack {
        itemStack.editMeta(Consumer { meta: ItemMeta? ->
            val data = meta!!.persistentDataContainer
            data.set(CUSTOM_ITEM_IDENTIFIER, PersistentDataType.STRING, customItem.key().toString())
            data.set(CUSTOM_ITEM_VERSION, PersistentDataType.INTEGER, customItem.version())
            // Use the new CustomModelDataComponent API instead of the deprecated setCustomModelData(Integer).
            val customModelDataComponent = meta.customModelDataComponent
            customModelDataComponent.floats = listOf(customItem.customModelData().toFloat())
            meta.setCustomModelDataComponent(customModelDataComponent)
        })

        DurabilityManager.initializeOrUpdateMax(customItem, itemStack)
        return customItem.updateItemStack(itemStack)
    }

    /**
     * Returns the resourceKey key and version number of the stored custom item tag on the given
     * item, if any. Returns null if none was found or the given item stack was null.
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

    /** Creates a new item stack with a single item of this custom item.  */
    @JvmStatic
    fun newStack(customItemKey: String): ItemStack {
        return newStack(customItemKey, 1)
    }

    /** Creates a new item stack with the given number of items of this custom item.  */
    fun newStack(customItemKey: String, amount: Int): ItemStack {
        val registry = Core.instance()?.itemRegistry()
            ?: throw IllegalStateException("CustomItemRegistry is not initialized")
        val ci = registry.get(NamespacedKey.fromString(customItemKey))
            ?: throw IllegalArgumentException("Unknown custom item: $customItemKey")
        return newStack(ci, amount)
    }

    /** Creates a new item stack with a single item of this custom item.  */
    @JvmStatic
    fun newStack(customItem: CustomItem): ItemStack {
        return newStack(customItem, 1)
    }

    /** Creates a new item stack with the given number of items of this custom item.  */
    @JvmStatic
    fun newStack(customItem: CustomItem, amount: Int): ItemStack {
        val itemStack = ItemStack(customItem.baseMaterial()!!, amount)
        itemStack.editMeta(Consumer { meta: ItemMeta? -> meta!!.itemName(customItem.displayName()) })
        return updateItemStack(customItem, itemStack)
    }

    /**
     * This function is called to convert an existing item stack of any form to this custom item
     * type, without losing metadata such as name, enchantments, etc. This is for example useful to
     * convert a diamond something into a netherite something, when those two items are different
     * CustomItem definitions but otherwise share attributes and functionality.
     */
    @JvmStatic
    fun convertExistingStack(customItem: CustomItem, itemStack: ItemStack): ItemStack {
        var itemStack = itemStack
        itemStack = itemStack.clone().withType(customItem.baseMaterial()!!)
        return updateItemStack(customItem, itemStack)
    }
}
