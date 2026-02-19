package org.oddlama.vane.regions.event;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.Regions;
import org.oddlama.vane.regions.region.RoleSetting;

public class RegionRoleSettingEnforcer extends Listener<Regions> {

    public RegionRoleSettingEnforcer(Context<Regions> context) {
        super(context);
    }

    public boolean checkSettingAt(
        final Location location,
        final Player player,
        final RoleSetting setting,
        final boolean checkAgainst
    ) {
        final var region = getModule().regionAt(location);
        if (region == null) {
            return false;
        }

        final var group = region.regionGroup(getModule());
        return group.getRole(player.getUniqueId()).getSetting(setting) == checkAgainst;
    }

    public boolean checkSettingAt(
        final Block block,
        final Player player,
        final RoleSetting setting,
        final boolean checkAgainst
    ) {
        final var region = getModule().regionAt(block);
        if (region == null) {
            return false;
        }

        final var group = region.regionGroup(getModule());
        return group.getRole(player.getUniqueId()).getSetting(setting) == checkAgainst;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        // Prevent breaking of region blocks
        if (checkSettingAt(event.getBlock(), event.getPlayer(), RoleSetting.BUILD, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        // Prevent (re-)placing of region blocks
        if (checkSettingAt(event.getBlock(), event.getPlayer(), RoleSetting.BUILD, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        final var damaged = event.getEntity();
        final var damager = event.getDamager();

        switch (damaged.getType()) {
            default:
                return;
            case ARMOR_STAND: {
                if (!(damager instanceof Player)) {
                    break;
                }

                final var playerDamager = (Player) damager;
                if (checkSettingAt(damaged.getLocation().getBlock(), playerDamager, RoleSetting.BUILD, false)) {
                    event.setCancelled(true);
                }
                return;
            }
            case ITEM_FRAME: {
                if (!(damager instanceof Player)) {
                    break;
                }

                final var playerDamager = (Player) damager;
                final var itemFrame = (ItemFrame) damaged;
                final var item = itemFrame.getItem();
                if (item.getType() != Material.AIR) {
                    // This is a player taking the item out of an item-frame
                    if (
                        checkSettingAt(damaged.getLocation().getBlock(), playerDamager, RoleSetting.CONTAINER, false)
                    ) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingBreakByEntity(final HangingBreakByEntityEvent event) {
        final Entity remover = event.getRemover();
        Player player = null;

        if (remover instanceof Player) {
            player = (Player) remover;
        } else if (remover instanceof Projectile) {
            final var projectile = (Projectile) remover;
            final var shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                player = (Player) shooter;
            }
        }

        if (player != null && checkSettingAt(event.getEntity().getLocation(), player, RoleSetting.BUILD, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingPlace(final HangingPlaceEvent event) {
        if (checkSettingAt(event.getEntity().getLocation(), event.getPlayer(), RoleSetting.BUILD, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerArmorStandManipulate(final PlayerArmorStandManipulateEvent event) {
        if (checkSettingAt(event.getRightClicked().getLocation(), event.getPlayer(), RoleSetting.BUILD, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent event) {
        if (checkSettingAt(event.getBlockClicked(), event.getPlayer(), RoleSetting.BUILD, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerBucketFill(final PlayerBucketFillEvent event) {
        if (checkSettingAt(event.getBlockClicked(), event.getPlayer(), RoleSetting.BUILD, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        final var entity = event.getRightClicked();
        if (entity.getType() != EntityType.ITEM_FRAME) {
            return;
        }

        // Place or rotate item
        if (checkSettingAt(entity.getLocation(), event.getPlayer(), RoleSetting.CONTAINER, false)) {
            event.setCancelled(true);
        }
    }

    // The EventPriority is HIGH, so this is executed AFTER the portals try
    // to activate, which is a seperate permission.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final var player = event.getPlayer();
        final var block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        switch (event.getAction()) {
            default:
                return;
            case PHYSICAL: {
                if (Tag.PRESSURE_PLATES.isTagged(block.getType())) {
                    if (checkSettingAt(block, player, RoleSetting.USE, false)) {
                        event.setCancelled(true);
                    }
                } else if (block.getType() == Material.TRIPWIRE) {
                    if (checkSettingAt(block, player, RoleSetting.USE, false)) {
                        event.setCancelled(true);
                    }
                }
                return;
            }
            case RIGHT_CLICK_BLOCK: {
                if (checkSettingAt(block, player, RoleSetting.USE, false)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInventoryOpen(final InventoryOpenEvent event) {
        // Only relevant if viewing should be prohibited, too.
        if (!getModule().configProhibitViewingContainers) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        final var inventory = event.getInventory();
        if (inventory.getLocation() == null || inventory.getHolder() == null) {
            // Inventory is virtual / transient
            return;
        }

        final var holder = inventory.getHolder();
        if (holder instanceof DoubleChest || holder instanceof Container || holder instanceof Minecart) {
            if (checkSettingAt(inventory.getLocation(), player, RoleSetting.CONTAINER, false)) {
                event.setCancelled(true);
            }
        }
    }

    public void onPlayerInventoryInteract(final InventoryInteractEvent event) {
        final var clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) {
            return;
        }

        final var inventory = event.getInventory();
        if (inventory.getLocation() == null || inventory.getHolder() == null) {
            // Inventory is virtual / transient
            return;
        }

        final var holder = inventory.getHolder();
        if (holder instanceof DoubleChest || holder instanceof Container || holder instanceof Minecart) {
            if (checkSettingAt(inventory.getLocation(), (Player) clicker, RoleSetting.CONTAINER, false)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInventoryClick(final InventoryClickEvent event) {
        onPlayerInventoryInteract(event);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInventoryDrag(final InventoryDragEvent event) {
        onPlayerInventoryInteract(event);
    }
}
