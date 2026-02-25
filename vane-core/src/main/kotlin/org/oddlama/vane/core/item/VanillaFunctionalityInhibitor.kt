package org.oddlama.vane.core.item

import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemMendEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.SmithingRecipe
import org.bukkit.inventory.meta.ItemMeta
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.MaterialUtil.isTillable
import java.util.function.Consumer

// TODO recipe book click event
class VanillaFunctionalityInhibitor(context: Context<Core?>?) : Listener<Core?>(context) {
    private fun inhibit(customItem: CustomItem?, behavior: InhibitBehavior?): Boolean {
        return customItem != null && customItem.enabled() && customItem.inhibitedBehaviors().contains(behavior)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPathfind(event: EntityTargetEvent) {
        if (event.reason != EntityTargetEvent.TargetReason.TEMPT) {
            return
        }

        if (event.target is Player) {
            val player = event.target as Player
            val customItemMain: CustomItem? = module!!.itemRegistry()?.get(player.inventory.itemInMainHand)
            val customItemOff: CustomItem? = module!!.itemRegistry()?.get(player.inventory.itemInOffHand)

            if (inhibit(customItemMain, InhibitBehavior.TEMPT) || inhibit(customItemOff, InhibitBehavior.TEMPT)) {
                event.isCancelled = true
            }
        }
    }

    // Prevent custom hoe items from tilling blocks
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerHoeRightClickBlock(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        // Only when clicking a tillable block
        if (!isTillable(event.clickedBlock!!.type)) {
            return
        }

        val player = event.getPlayer()
        val item = player.equipment.getItem(event.hand!!)
        if (inhibit(module!!.itemRegistry()?.get(item), InhibitBehavior.HOE_TILL)) {
            event.setCancelled(true)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe
        if (recipe !is Keyed) {
            return
        }

        // Only consider canceling minecraft's recipes
        if (recipe.key.namespace != "minecraft") {
            return
        }

        for (item in event.inventory.matrix) {
            if (inhibit(module!!.itemRegistry()?.get(item), InhibitBehavior.USE_IN_VANILLA_RECIPE)) {
                event.inventory.result = null
                return
            }
        }
    }

    // Prevent custom items from being used in smithing by default. They have to override this event
    // to allow it.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPrepareSmithing(event: PrepareSmithingEvent) {
        val item = event.inventory.inputEquipment
        val recipe = event.inventory.recipe
        if (recipe !is Keyed) {
            return
        }

        // Only consider canceling Minecraft's recipes
        if (recipe.key.namespace != "minecraft") {
            return
        }

        if (inhibit(module!!.itemRegistry()?.get(item), InhibitBehavior.USE_IN_VANILLA_RECIPE)) {
            event.inventory.result = null
        }
    }

    // If the result of a smithing recipe is a custom item, copy and merge input NBT data.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPrepareSmithingCopyNbt(event: PrepareSmithingEvent) {
        var result = event.result
        val recipe = event.inventory.recipe
        if (result == null || (recipe !is SmithingRecipe) || !recipe.willCopyNbt()) {
            return
        }

        // Actually use a recipe result, as copynbt has already modified the result
        result = recipe.result
        val customItemResult = module!!.itemRegistry()?.get(result) ?: return

        val input = event.inventory.inputEquipment
        val inputComponents = CraftItemStack.asNMSCopy(input).getComponents()
        val nmsResult = CraftItemStack.asNMSCopy(result)
        nmsResult.applyComponents(inputComponents)

        event.result = customItemResult.convertExistingStack(CraftItemStack.asCraftMirror(nmsResult))
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val a = event.inventory.firstItem
        val b = event.inventory.secondItem

        // Always prevent custom item repair with the custom item base material
        // if it is not also a matching custom item.
        // TODO: what about inventory based item repair?
        if (a != null && b != null && a.type == b.type) {
            // Disable the result unless a and b are instances of the same custom item.
            val customItemA = module!!.itemRegistry()?.get(a)
            val customItemB = module!!.itemRegistry()?.get(b)
            if (customItemA != null && customItemA !== customItemB) {
                event.result = null
                return
            }
        }

        val r = event.inventory.result
        if (r != null) {
            val customItemR = module!!.itemRegistry()?.get(r)
            val didEdit = booleanArrayOf(true)
            r.editMeta(Consumer { meta: ItemMeta? ->
                if (a != null && inhibit(customItemR, InhibitBehavior.NEW_ENCHANTS)) {
                    for (ench in r.enchantments.keys) {
                        if (!a.enchantments.containsKey(ench)) {
                            meta!!.removeEnchant(ench)
                            didEdit[0] = true
                        }
                    }
                }
                if (inhibit(customItemR, InhibitBehavior.MEND)) {
                    meta!!.removeEnchant(Enchantment.MENDING)
                    didEdit[0] = true
                }
            })

            if (didEdit[0]) {
                event.result = r
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onItemMend(event: PlayerItemMendEvent) {
        val item = event.item
        val customItem = module!!.itemRegistry()?.get(item)

        // No repairing for mending inhibited items.
        if (inhibit(customItem, InhibitBehavior.MEND)) {
            event.isCancelled = true
        }
    }

    // Prevent netherite items from burning, as they are made of netherite
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onItemBurn(event: EntityDamageEvent) {
        // Only burn damage on dropped items
        if (event.getEntity().type != EntityType.ITEM) {
            return
        }

        when (event.cause) {
            DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.LAVA -> {}
            else -> return
        }

        val entity = event.getEntity()
        if (entity !is Item) {
            return
        }

        val item = entity.itemStack
        if (inhibit(module!!.itemRegistry()?.get(item), InhibitBehavior.ITEM_BURN)) {
            event.isCancelled = true
        }
    }

    // Deny off-hand usage if the main hand is a custom item that inhibits off-hand usage.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.OFF_HAND) {
            return
        }

        val player = event.getPlayer()
        val mainItem = player.equipment.getItem(EquipmentSlot.HAND)
        val mainCustomItem = module!!.itemRegistry()?.get(mainItem)
        if (inhibit(mainCustomItem, InhibitBehavior.USE_OFFHAND)) {
            event.setUseItemInHand(Event.Result.DENY)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onDispense(event: BlockDispenseEvent) {
        if (event.getBlock().type != Material.DISPENSER) {
            return
        }

        val customItem = module!!.itemRegistry()?.get(event.item)
        if (inhibit(customItem, InhibitBehavior.DISPENSE)) {
            event.isCancelled = true
        }
    }
}
