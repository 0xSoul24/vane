package org.oddlama.vane.admin

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.entity.EntityCombustByEntityEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.*
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context

class WorldProtection(context: Context<Admin?>) : Listener<Admin?>(
    context.group(
        "WorldProtection",
        "Enable world protection. This will prevent anyone from modifying the world if they don't have the permission '" +
                PERMISSION_NAME +
                "'.",
        false
    )
) {
    private val permission = Permission(
        PERMISSION_NAME,
        "Allow player to modify world",
        PermissionDefault.OP
    )

    init {
        module!!.registerPermission(permission)
    }

    fun denyModifyWorld(entity: Entity?): Boolean {
        if (entity !is Player) {
            return false
        }

        return !entity.hasPermission(permission)
    }

    /* ************************ blocks ************************ */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (denyModifyWorld(event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (denyModifyWorld(event.getPlayer())) {
            event.isCancelled = true
        }
    }

    /* ************************ enchantment ************************ */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPrepareItemEnchant(event: PrepareItemEnchantEvent) {
        if (denyModifyWorld(event.enchanter)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onItemEnchant(event: EnchantItemEvent) {
        if (denyModifyWorld(event.enchanter)) {
            event.isCancelled = true
        }
    }

    /* ************************ entity ************************ */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityCombustByEntity(event: EntityCombustByEntityEvent) {
        if (denyModifyWorld(event.combuster)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event is EntityDamageByEntityEvent) {
            if (denyModifyWorld(event.damager)) {
                event.isCancelled = true
            } else if (denyModifyWorld(event.getEntity())) {
                event.isCancelled = true
            }
        } else if (denyModifyWorld(event.getEntity())) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityFoodLevelChange(event: FoodLevelChangeEvent) {
        if (denyModifyWorld(event.getEntity())) {
            event.isCancelled = true
        }
    }

    /* ************************ hanging ************************ */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent) {
        if (denyModifyWorld(event.remover)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onHangingPlace(event: HangingPlaceEvent) {
        if (denyModifyWorld(event.player)) {
            event.isCancelled = true
        }
    }

    /* ************************ inventory ************************ */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onItemCraft(event: CraftItemEvent) {
        if (denyModifyWorld(event.whoClicked)) {
            event.isCancelled = true
        }
    }

    /* ************************ player ************************ */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        if (denyModifyWorld(event.getPlayer())) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (denyModifyWorld(event.getPlayer())) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerBucketFill(event: PlayerBucketFillEvent) {
        if (denyModifyWorld(event.getPlayer())) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerEditBook(event: PlayerEditBookEvent) {
        if (denyModifyWorld(event.getPlayer())) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (denyModifyWorld(event.getPlayer())) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (denyModifyWorld(event.getPlayer())) {
            event.setCancelled(true)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerShearEntity(event: PlayerShearEntityEvent) {
        if (denyModifyWorld(event.getPlayer())) {
            event.isCancelled = true
        }
    }

    companion object {
        private const val PERMISSION_NAME = "vane.admin.modify_world"
    }
}
