package org.oddlama.vane.admin;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class SpawnProtection extends Listener<Admin> {

    private static final String PERMISSION_NAME = "vane.admin.bypass_spawn_protection";
    private Permission permission = new Permission(
        PERMISSION_NAME,
        "Allow player to bypass spawn protection",
        PermissionDefault.OP
    );

    @ConfigBoolean(def = true, desc = "Allow interaction events at spawn (buttons, levers, etc.).")
    private boolean configAllowInteraction;

    @ConfigInt(def = 64, min = 0, desc = "Radius to protect.")
    private int configRadius;

    @ConfigString(def = "world", desc = "The spawn world.")
    private String configWorld;

    @ConfigBoolean(def = true, desc = "Use world's spawn location instead of the specified center coordinates.")
    private boolean configUseSpawnLocation;

    @ConfigInt(def = 0, desc = "Center X coordinate.")
    private int configX;

    @ConfigInt(def = 0, desc = "Center Z coordinate.")
    private int configZ;

    public SpawnProtection(Context<Admin> context) {
        super(
            context.groupDefaultDisabled(
                "SpawnProtection",
                "Enable spawn protection. Slightly more sophisticated than the vanilla spawn protection, if you need even more control, use regions. This will prevent anyone from modifying the spawn of the world if they don't have the permission '" +
                PERMISSION_NAME +
                "'."
            )
        );
        getModule().registerPermission(permission);
    }

    private Location spawnCenter = null;

    @Override
    public void onConfigChange() {
        spawnCenter = null;
        scheduleNextTick(() -> {
            final var world = getModule().getServer().getWorld(configWorld);
            if (world == null) {
                // todo print error and show valid worlds.
                getModule()
                    .getLog().warning(
                        "The world \"" + configWorld + "\" configured for spawn-protection could not be found."
                    );
                getModule().getLog().warning("These are the names of worlds existing on this server:");
                for (final var w : getModule().getServer().getWorlds()) {
                    getModule().getLog().warning("  \"" + w.getName() + "\"");
                }
                spawnCenter = null;
            } else {
                if (configUseSpawnLocation) {
                    spawnCenter = world.getSpawnLocation();
                    spawnCenter.setY(0);
                } else {
                    spawnCenter = new Location(world, configX, 0, configZ);
                }
            }
        });
    }

    public boolean denyModifySpawn(final Block block, final Entity entity) {
        return denyModifySpawn(block.getLocation(), entity);
    }

    public boolean denyModifySpawn(final Location location, final Entity entity) {
        if (spawnCenter == null || !(entity instanceof Player)) {
            return false;
        }

        final var dx = location.getX() - spawnCenter.getX();
        final var dz = location.getZ() - spawnCenter.getZ();
        final var distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > configRadius) {
            return false;
        }

        return !entity.hasPermission(permission);
    }

    /* ************************ blocks ************************ */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (denyModifySpawn(event.getBlock(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (denyModifySpawn(event.getBlock(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /* ************************ hanging ************************ */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (denyModifySpawn(event.getEntity().getLocation(), event.getRemover())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (denyModifySpawn(event.getEntity().getLocation(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /* ************************ player ************************ */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (denyModifySpawn(event.getRightClicked().getLocation(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (denyModifySpawn(event.getBlock(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (denyModifySpawn(event.getBlock(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!configAllowInteraction && denyModifySpawn(event.getRightClicked().getLocation(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (
            event.getClickedBlock() != null &&
            !configAllowInteraction &&
            denyModifySpawn(event.getClickedBlock(), event.getPlayer())
        ) {
            event.setCancelled(true);
        }
    }
}
