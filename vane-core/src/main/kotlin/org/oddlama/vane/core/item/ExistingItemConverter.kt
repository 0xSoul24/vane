package org.oddlama.vane.core.item

import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.item.CustomItemHelper.customItemTagsFromItemStack
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.module.Context
import java.util.function.Consumer

class ExistingItemConverter(context: Context<Core?>) : Listener<Core?>(context.namespace("existing_item_converter")) {
    private fun fromOldItem(itemStack: ItemStack): CustomItem? {
        val modelDataList = itemStack.itemMeta.customModelDataComponent.floats
        if (modelDataList.isEmpty() || modelDataList.firstOrNull() == null) {
            return null
        }

        // If lookups fail, we return null and nothing will be done.
        val newItemKey = when (modelDataList.firstOrNull()!!.toInt()) {
            7758190 -> "vane_trifles:wooden_sickle"
            7758191 -> "vane_trifles:stone_sickle"
            7758192 -> "vane_trifles:iron_sickle"
            7758193 -> "vane_trifles:golden_sickle"
            7758194 -> "vane_trifles:diamond_sickle"
            7758195 -> "vane_trifles:netherite_sickle"
            7758254, 7758255, 7758256, 7758257, 7758258, 7758259 -> "vane_trifles:file"
            7758318 -> "vane_trifles:empty_xp_bottle"
            7758382 -> "vane_trifles:small_xp_bottle"
            7758383 -> "vane_trifles:medium_xp_bottle"
            7758384 -> "vane_trifles:large_xp_bottle"
            7758446 -> "vane_trifles:home_scroll"
            7758510 -> "vane_trifles:unstable_scroll"
            7758574 -> "vane_trifles:reinforced_elytra"
            7823726 -> "vane_enchantments:ancient_tome"
            7823727 -> "vane_enchantments:enchanted_ancient_tome"
            7823790 -> "vane_enchantments:ancient_tome_of_knowledge"
            7823791 -> "vane_enchantments:enchanted_ancient_tome_of_knowledge"
            7823854 -> "vane_enchantments:ancient_tome_of_the_gods"
            7823855 -> "vane_enchantments:enchanted_ancient_tome_of_the_gods"
            else -> null
        }

        if (newItemKey == null) {
            return null
        }
        return module!!.itemRegistry()?.get(NamespacedKey.fromString(newItemKey))
    }

    private fun processInventory(inventory: Inventory) {
        val contents = inventory.contents
        var changed = 0

        for (i in contents.indices) {
            val item = contents[i] ?: continue
            if (!item.hasItemMeta()) {
                continue
            }

            val customItem = module!!.itemRegistry()?.get(item)
            if (customItem == null) {
                // Determine if the item stack should be converted to a custom item from a legacy
                // definition
                val convertToCustomItem = fromOldItem(item) ?: continue

                val converted = convertToCustomItem.convertExistingStack(item)
                contents[i] = converted
                contents[i]!!.editMeta(Consumer { meta: ItemMeta? -> meta!!.itemName(convertToCustomItem.displayName()) })
                module!!.enchantmentManager?.updateEnchantedItem(converted!!)
                module!!.log.info("Converted legacy item to " + convertToCustomItem.key())
                ++changed
                continue
            }

            // Remove obsolete custom items
            if (module!!.itemRegistry()?.shouldRemove(customItem.key()) == true) {
                contents[i] = null
                module!!.log.info("Removed obsolete item " + customItem.key())
                ++changed
                continue
            }

            // Update custom items to a new version, or if another detectable property changed.
            val keyAndVersion = customItemTagsFromItemStack(item)
            val meta = item.itemMeta
            val modelDataList = meta.customModelDataComponent.floats
            val modelDataInt = modelDataList.firstOrNull()?.toInt()

            if (modelDataInt == null || modelDataInt != customItem.customModelData() || item.type != customItem.baseMaterial() || keyAndVersion!!.getRight() != customItem.version()) {
                // Also includes durability max update.
                contents[i] = customItem.convertExistingStack(item)
                module!!.log.info("Updated item " + customItem.key())
                ++changed
                continue
            }

            // Update maximum durability on existing items if changed.
            val damageableMeta = contents[i]!!.itemMeta as Damageable
            val maxDamage = if (damageableMeta.hasMaxDamage())
                damageableMeta.maxDamage
            else
                item.type.getMaxDurability().toInt()
            val correctMaxDamage = if (customItem.durability() == 0)
                item.type.getMaxDurability()
                    .toInt()
            else
                customItem.durability()
            if (maxDamage != correctMaxDamage ||
                meta.persistentDataContainer.has(DurabilityManager.ITEM_DURABILITY_DAMAGE)
            ) {
                module!!.log.info("Updated item durability " + customItem.key())
                DurabilityManager.updateDamage(customItem, contents[i]!!)
                ++changed
                continue
            }
        }

        if (changed > 0) {
            inventory.contents = contents
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        processInventory(event.getPlayer().inventory)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        // Catches enderchests, and inventories by other plugins
        processInventory(event.inventory)
    }
}
