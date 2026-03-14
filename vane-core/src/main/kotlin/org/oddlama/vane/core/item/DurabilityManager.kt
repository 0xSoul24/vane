package org.oddlama.vane.core.item

import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.ItemUtil.hasSentinel
import org.oddlama.vane.util.StorageUtil.namespacedKey

/**
 * Manages durability metadata synchronization for custom items.
 *
 * @param context listener context.
 */
class DurabilityManager(context: Context<Core?>?) : Listener<Core?>(context) {
    /**
     * Synchronizes custom durability data when an item takes damage.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onItemDamage(event: PlayerItemDamageEvent) {
        val customItem = module!!.itemRegistry()?.get(event.item) ?: return
        updateDamage(customItem, event.item)
    }

    /**
     * Static durability helpers and persistent-data keys.
     */
    companion object {
        /**
         * Persistent-data key for maximum durability.
         */
        val ITEM_DURABILITY_MAX: NamespacedKey = namespacedKey("vane", "durability.max")

        /**
         * Persistent-data key for current durability damage.
         */
        val ITEM_DURABILITY_DAMAGE: NamespacedKey = namespacedKey("vane", "durability.damage")

        /**
         * Lore sentinel key for durability override entries.
         */
        private val SENTINEL = namespacedKey("vane", "durability_override_lore")

        /**
         * Returns whether a lore component is durability metadata lore.
         */
        private fun isDurabilityLore(component: Component?): Boolean = hasSentinel(component, SENTINEL)

        /**
         * Removes durability lore entries from an item.
         */
        private fun removeLore(itemStack: ItemStack) {
            val lore = itemStack.lore() ?: return
            lore.removeIf { isDurabilityLore(it) }
            itemStack.lore(lore.ifEmpty { null })
        }

        /**
         * Applies effective damage while respecting unbreakable items.
         */
        private fun setDamageAndUpdateItem(customItem: CustomItem, itemStack: ItemStack, damage: Int) {
            val actualDamage = if (itemStack.itemMeta?.isUnbreakable == true) 0 else damage
            setDamageAndMaxDamage(customItem, itemStack, actualDamage)
        }

        /**
         * Initializes or recalculates max durability state on an item.
         */
        fun initializeOrUpdateMax(customItem: CustomItem, itemStack: ItemStack): Boolean {
            val pdc = itemStack.itemMeta?.persistentDataContainer ?: return false
            val oldDamage = pdc.getOrDefault(ITEM_DURABILITY_DAMAGE, PersistentDataType.INTEGER, -1)

            itemStack.editMeta { meta ->
                meta.persistentDataContainer.remove(ITEM_DURABILITY_DAMAGE)
                meta.persistentDataContainer.remove(ITEM_DURABILITY_MAX)
            }

            if (customItem.durability() <= 0) {
                removeLore(itemStack)
                return false
            }

            val actualDamage = when {
                oldDamage != -1 -> oldDamage
                itemStack.itemMeta is Damageable -> {
                    val damageMeta = itemStack.itemMeta as Damageable
                    val visualMax = itemStack.type.maxDurability.toInt()
                    val pct = if (visualMax > 0) damageMeta.damage.toDouble() / visualMax else 0.0
                    (customItem.durability() * pct).toInt()
                }
                else -> 0
            }

            setDamageAndUpdateItem(customItem, itemStack, actualDamage)
            return true
        }

        /**
         * Reconciles durability metadata when an item changes durability source.
         */
        fun updateDamage(customItem: CustomItem, itemStack: ItemStack) {
            val meta = itemStack.itemMeta as? Damageable ?: return

            val newMaxDamage = if (customItem.durability() == 0) itemStack.type.maxDurability.toInt()
                               else customItem.durability()

            val data = meta.persistentDataContainer
            val hasPdc = data.has(ITEM_DURABILITY_DAMAGE) && data.has(ITEM_DURABILITY_MAX)

            val oldDamage = if (hasPdc) data.get(ITEM_DURABILITY_DAMAGE, PersistentDataType.INTEGER) ?: 0
                            else if (meta.hasDamage()) meta.damage else 0
            val oldMaxDamage = if (hasPdc) data.get(ITEM_DURABILITY_MAX, PersistentDataType.INTEGER) ?: itemStack.type.maxDurability.toInt()
                               else if (meta.hasMaxDamage()) meta.maxDamage else itemStack.type.maxDurability.toInt()

            itemStack.editMeta(Damageable::class.java) { imeta ->
                imeta.persistentDataContainer.remove(ITEM_DURABILITY_DAMAGE)
                imeta.persistentDataContainer.remove(ITEM_DURABILITY_MAX)
            }
            removeLore(itemStack)

            if (!hasPdc && oldMaxDamage == newMaxDamage) return

            setDamageAndMaxDamage(customItem, itemStack, scaleDamage(oldDamage, oldMaxDamage, newMaxDamage))
        }

        /**
         * Scales damage from one max-durability domain to another.
         */
        fun scaleDamage(oldDamage: Int, oldMaxDamage: Int, newMaxDamage: Int): Int =
            if (oldMaxDamage == newMaxDamage) oldDamage
            else (newMaxDamage * (oldDamage.toFloat() / oldMaxDamage.toFloat())).toInt()

        /**
         * Writes max damage and current damage to item meta.
         */
        fun setDamageAndMaxDamage(customItem: CustomItem, item: ItemStack, damage: Int): Boolean =
            item.editMeta(Damageable::class.java) { meta ->
                meta.setMaxDamage(if (customItem.durability() != 0) customItem.durability() else item.type.maxDurability.toInt())
                meta.damage = damage
            }
    }
}
