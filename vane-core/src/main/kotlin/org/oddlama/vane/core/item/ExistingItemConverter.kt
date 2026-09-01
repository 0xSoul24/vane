package org.oddlama.vane.core.item

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.item.CustomItemHelper.customItemTagsFromItemStack
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.module.Context

/**
 * Converts legacy or outdated item stacks in inventories to current custom-item formats.
 *
 * @param context listener context.
 */
class ExistingItemConverter(context: Context<Core?>) : Listener<Core?>(context.namespace("existing_item_converter")) {
    /**
     * Resolves legacy model data ids to currently registered custom items.
     */
    private fun fromOldItem(itemStack: ItemStack): CustomItem? {
        val modelDataInt = itemStack.itemMeta.customModelDataComponent.floats.firstOrNull()?.toInt()
            ?: return null

        // Newer mappings (1.21.4+) changed how custom-model-data float values map
        // to integer representations. Instead of maintaining a hardcoded list of
        // legacy integers, resolve the target custom item dynamically by
        // comparing the legacy float->int representation of registered items.
        val registry = module!!.itemRegistry() ?: return null
        for (ci in registry.all()) {
            if (ci.customModelData().toFloat().toInt() == modelDataInt) return ci
        }

        return null
    }

    /**
     * Processes and migrates all item stacks in an inventory.
     */
    private fun processInventory(inventory: Inventory) {
        val contents = inventory.contents
        var changed = 0

        for (i in contents.indices) {
            val item = contents[i] ?: continue
            if (!item.hasItemMeta()) continue

            val customItem = module!!.itemRegistry()?.get(item)
            if (customItem == null) {
                val convertToCustomItem = fromOldItem(item) ?: continue
                val converted = convertToCustomItem.convertExistingStack(item)
                contents[i] = converted
                contents[i]!!.editMeta { it.itemName(convertToCustomItem.displayName()) }
                module!!.enchantmentManager?.updateEnchantedItem(converted!!)
                module!!.log.info("Converted legacy item to ${convertToCustomItem.key()}")
                ++changed
                continue
            }

            if (module!!.itemRegistry()?.shouldRemove(customItem.key()) == true) {
                contents[i] = null
                module!!.log.info("Removed obsolete item ${customItem.key()}")
                ++changed
                continue
            }

            val keyAndVersion = customItemTagsFromItemStack(item)
            val meta = item.itemMeta
            val modelDataInt = meta.customModelDataComponent.floats.firstOrNull()?.toInt()

            if (modelDataInt == null ||
                modelDataInt != customItem.customModelData() ||
                item.type != customItem.baseMaterial() ||
                keyAndVersion?.second != customItem.version()
            ) {
                contents[i] = customItem.convertExistingStack(item)
                module!!.log.info("Updated item ${customItem.key()}")
                ++changed
                continue
            }

            val damageableMeta = contents[i]!!.itemMeta as Damageable
            val maxDamage = if (damageableMeta.hasMaxDamage()) damageableMeta.maxDamage
            else item.type.maxDurability.toInt()
            val correctMaxDamage = if (customItem.durability() == 0) item.type.maxDurability.toInt()
            else customItem.durability()

            if (maxDamage != correctMaxDamage ||
                meta.persistentDataContainer.has(DurabilityManager.ITEM_DURABILITY_DAMAGE)
            ) {
                module!!.log.info("Updated item durability ${customItem.key()}")
                DurabilityManager.updateDamage(customItem, contents[i]!!)
                ++changed
                continue
            }
        }

        if (changed > 0) inventory.contents = contents
    }

    /**
     * Converts items in player inventory on join.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) = processInventory(event.player.inventory)

    /**
     * Converts items in opened inventories.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryOpen(event: InventoryOpenEvent) = processInventory(event.inventory)
}
