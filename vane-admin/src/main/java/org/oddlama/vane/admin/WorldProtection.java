package org.oddlama.vane.admin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class WorldProtection extends Listener<Admin> {

    private static final String PERMISSION_NAME = "vane.admin.modify_world";
    private Permission permission = new Permission(
        PERMISSION_NAME,
        "Allow player to modify world",
        PermissionDefault.OP
    );

    public WorldProtection(Context<Admin> context) {
        super(
            context.group(
                "WorldProtection",
                "Enable world protection. This will prevent anyone from modifying the world if they don't have the permission '" +
                PERMISSION_NAME +
                "'.",
                false
            )
        );
        getModule().registerPermission(permission);
    }

    public boolean denyModifyWorld(final Entity entity) {
        if (!(entity instanceof Player)) {
            return false;
        }

        return !entity.hasPermission(permission);
    }

    /* ************************ blocks ************************ */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (denyModifyWorld(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (denyModifyWorld(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /* ************************ enchantment ************************ */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        if (denyModifyWorld(event.getEnchanter())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemEnchant(EnchantItemEvent event) {
        if (denyModifyWorld(event.getEnchanter())) {
            event.setCancelled(true);
        }
    }

    /* ************************ entity ************************ */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityCombustByEntity(EntityCombustByEntityEvent event) {
        if (denyModifyWorld(event.getCombuster())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            final var damageEvent = (EntityDamageByEntityEvent) event;
            if (denyModifyWorld(damageEvent.getDamager())) {
                event.setCancelled(true);
            } else if (denyModifyWorld(damageEvent.getEntity())) {
                event.setCancelled(true);
            }
        } else if (denyModifyWorld(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityFoodLevelChange(FoodLevelChangeEvent event) {
        if (denyModifyWorld(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /* ************************ hanging ************************ */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (denyModifyWorld(event.getRemover())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (denyModifyWorld(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /* ************************ inventory ************************ */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemCraft(CraftItemEvent event) {
        if (denyModifyWorld(event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    /* ************************ player ************************ */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (denyModifyWorld(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (denyModifyWorld(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (denyModifyWorld(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        if (denyModifyWorld(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (denyModifyWorld(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (denyModifyWorld(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        if (denyModifyWorld(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
