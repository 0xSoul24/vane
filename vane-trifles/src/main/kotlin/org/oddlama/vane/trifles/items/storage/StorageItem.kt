package org.oddlama.vane.trifles.items.storage

import org.bukkit.Sound
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.StorageGroup
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.PlayerUtil
import java.util.function.Consumer

abstract class StorageItem(context: Context<Trifles?>) : org.oddlama.vane.core.item.CustomItem<Trifles?>(context) {

    override fun updateItemStack(itemStack: ItemStack): ItemStack {
        // Add custom storage tag to every created storage item
        itemStack.editMeta(Consumer { meta: ItemMeta? ->
            meta!!.persistentDataContainer
                .set(StorageGroup.STORAGE_IS_OPEN, PersistentDataType.BOOLEAN, false)
        })
        return itemStack
    }

    // ignoreCancelled = false to catch right-click-air events
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (!event.hasItem() || event.useItemInHand() == Event.Result.DENY) {
            return
        }

        // Any right click to open
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) {
            return
        }

        // Assert this is a matching custom item
        val player = event.getPlayer()
        val item = player.equipment.getItem(event.hand!!)
        val customItem = module!!.core?.itemRegistry()?.get(item)
        if (customItem?.javaClass != this::class.java || !customItem.enabled()) {
            return
        }

        // Set all storage items in inventory as closed
        for (invItem in player.inventory.contents) {
            if (invItem != null && invItem.hasItemMeta()) {
                invItem.editMeta(Consumer { meta: ItemMeta? ->
                    if (meta!!.persistentDataContainer.has(StorageGroup.STORAGE_IS_OPEN)) {
                        meta.persistentDataContainer
                            .set(StorageGroup.STORAGE_IS_OPEN, PersistentDataType.BOOLEAN, false)
                    }
                })
            }
        }

        // Tag storage item in hand as opened
        item.editMeta(Consumer { meta: ItemMeta? ->
            meta!!.persistentDataContainer
                .set(StorageGroup.STORAGE_IS_OPEN, PersistentDataType.BOOLEAN, true)
        })

        // Never use anything else (e.g., offhand)
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)

        if (module!!.storageGroup.openBlockStateInventory(player, item)) {
            player.world.playSound(player, Sound.ITEM_BUNDLE_DROP_CONTENTS, 1.0f, 1.2f)
            PlayerUtil.swingArm(player, event.hand!!)
        }
    }
}
