package org.oddlama.vane.core.item

import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.ItemUtil.hasSentinel
import org.oddlama.vane.util.StorageUtil.namespacedKey
import java.util.function.Consumer

// TODO: what about inventory based item repair?
class DurabilityManager(context: Context<Core?>?) : Listener<Core?>(context) {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onItemDamage(event: PlayerItemDamageEvent) {
        val item = event.item
        val customItem = module!!.itemRegistry()?.get(item) ?: return

        // Ignore normal items

        updateDamage(customItem, item)
    }

    companion object {
        val ITEM_DURABILITY_MAX: NamespacedKey = namespacedKey("vane", "durability.max")
        val ITEM_DURABILITY_DAMAGE: NamespacedKey = namespacedKey("vane", "durability.damage")

        private val SENTINEL = namespacedKey("vane", "durability_override_lore")

        /** Returns true if the given component is associated to our custom durability.  */
        private fun isDurabilityLore(component: Component?): Boolean {
            return hasSentinel(component, SENTINEL)
        }

        /** Removes associated lore from an item.  */
        private fun removeLore(itemStack: ItemStack) {
            val lore = itemStack.lore()
            if (lore != null) {
                lore.removeIf { component: Component? -> isDurabilityLore(component) }
                if (lore.isNotEmpty()) {
                    itemStack.lore(lore)
                } else {
                    itemStack.lore(null)
                }
            }
        }

        /**
         * Sets the item's damage regarding our custom durability. The durability will get clamped to
         * plausible values. Damage values >= max will result in item breakage. The maximum value will
         * be taken from the item tag if it exists.
         */
        private fun setDamageAndUpdateItem(
            customItem: CustomItem,
            itemStack: ItemStack,
            damage: Int
        ) {
            // Honor unbreakable flag
            var damage = damage
            val roMeta = itemStack.itemMeta
            if (roMeta != null && roMeta.isUnbreakable) {
                damage = 0
            }
            setDamageAndMaxDamage(customItem, itemStack, damage)
        }

        /**
         * Initializes damage on the item, or removes them if custom durability is disabled for the
         * given custom item.
         */
        fun initializeOrUpdateMax(customItem: CustomItem, itemStack: ItemStack): Boolean {
            // Remember damage if set.
            val pdc: PersistentDataContainer = itemStack.itemMeta?.persistentDataContainer
                ?: return false

            // Use Java Integer explicitly to satisfy the generic signature and avoid Kotlin inference issues.
            val oldDamage = pdc.getOrDefault(ITEM_DURABILITY_DAMAGE, PersistentDataType.INTEGER, Integer.valueOf(-1))

            // First, remove all components.
            itemStack.editMeta(Consumer { meta: ItemMeta? ->
                val data = meta!!.persistentDataContainer
                data.remove(ITEM_DURABILITY_DAMAGE)
                data.remove(ITEM_DURABILITY_MAX)
            })

            // The item has no durability anymore. Remove leftover lore and return.
            if (customItem.durability() <= 0) {
                removeLore(itemStack)
                return false
            }

            val actualDamage: Int
            if (oldDamage == -1) {
                if (itemStack.itemMeta is Damageable) {
                    // If there was no old damage value, initialize proportionally by visual damage.
                    val damageMeta = itemStack.itemMeta as Damageable
                    val visualMax = itemStack.type.getMaxDurability()
                    val damagePercentage = if (visualMax > 0) damageMeta.damage.toDouble() / visualMax else 0.0
                    actualDamage = (customItem.durability() * damagePercentage).toInt()
                } else {
                    // There was no old damage value, but the item has no visual durability.
                    // Initialize with max durability.
                    actualDamage = 0
                }
            } else {
                // Keep old damage.
                actualDamage = oldDamage
            }

            setDamageAndUpdateItem(customItem, itemStack, actualDamage)

            return true
        }

        /** Update existing max damage to match the configuration  */
        fun updateDamage(customItem: CustomItem, itemStack: ItemStack) {
            val meta = itemStack.itemMeta
            if (meta !is Damageable) return  // everything should be damageable now


            var updated = false
            val data: PersistentDataContainer = meta.persistentDataContainer

            val newMaxDamage = if (customItem.durability() == 0)
                itemStack.type.getMaxDurability()
                    .toInt()
            else
                customItem.durability()

            val oldDamage: Int
            val oldMaxDamage: Int
            // if the item has damage in their data, get the value and remove it from PDC
            if (data.has(ITEM_DURABILITY_DAMAGE) && data.has(ITEM_DURABILITY_MAX)) {
                oldDamage = data.get(ITEM_DURABILITY_DAMAGE, PersistentDataType.INTEGER) ?: 0
                oldMaxDamage = data.get(ITEM_DURABILITY_MAX, PersistentDataType.INTEGER) ?: itemStack.type.getMaxDurability().toInt()
                updated = true
            } else {
                oldDamage = if (meta.hasDamage()) meta.damage else 0
                oldMaxDamage =
                    if (meta.hasMaxDamage()) meta.maxDamage else itemStack.type.getMaxDurability().toInt()
            }

            itemStack.editMeta(Damageable::class.java, Consumer { imeta: Damageable? ->
                val idata = imeta!!.persistentDataContainer
                idata.remove(ITEM_DURABILITY_DAMAGE)
                idata.remove(ITEM_DURABILITY_MAX)
            })

            removeLore(itemStack)

            if (!updated) updated = oldMaxDamage != newMaxDamage // only update if there was old data or a different

            // max
            // durability
            if (!updated) return  // and do nothing if nothing changed

            val newDamage: Int = scaleDamage(oldDamage, oldMaxDamage, newMaxDamage)
            setDamageAndMaxDamage(customItem, itemStack, newDamage)
        }

        fun scaleDamage(oldDamage: Int, oldMaxDamage: Int, newMaxDamage: Int): Int {
            return if (oldMaxDamage == newMaxDamage)
                oldDamage
            else (newMaxDamage * (oldDamage.toFloat() / oldMaxDamage.toFloat())).toInt()
        }

        fun setDamageAndMaxDamage(customItem: CustomItem, item: ItemStack, damage: Int): Boolean {
            return item.editMeta(Damageable::class.java, Consumer { meta: Damageable? ->
                if (customItem.durability() != 0) {
                    meta!!.setMaxDamage(customItem.durability())
                } else {
                    meta!!.setMaxDamage(item.type.getMaxDurability().toInt())
                }
                meta.damage = damage
            })
        }
    }
}
