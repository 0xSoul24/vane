package org.oddlama.vane.core.item

import org.bukkit.NamespacedKey
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

class ExistingItemConverter(context: Context<Core?>) : Listener<Core?>(context.namespace("existing_item_converter")) {
    private fun fromOldItem(itemStack: ItemStack): CustomItem? {
        val modelDataInt = itemStack.itemMeta.customModelDataComponent.floats.firstOrNull()?.toInt()
            ?: return null

        val newItemKey = when (modelDataInt) {
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
        } ?: return null

        return module!!.itemRegistry()?.get(NamespacedKey.fromString(newItemKey) ?: return null)
    }

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
                keyAndVersion?.getRight() != customItem.version()
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) = processInventory(event.player.inventory)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryOpen(event: InventoryOpenEvent) = processInventory(event.inventory)
}
