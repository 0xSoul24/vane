package org.oddlama.vane.regions.event;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.Regions;
import org.oddlama.vane.regions.region.EnvironmentSetting;

public class RegionEnvironmentSettingEnforcer extends Listener<Regions> {

    public RegionEnvironmentSettingEnforcer(Context<Regions> context) {
        super(context);
    }

    public boolean checkSettingAt(
        final Location location,
        final EnvironmentSetting setting,
        final boolean checkAgainst
    ) {
        final var region = getModule().regionAt(location);
        if (region == null) {
            return false;
        }

        final var group = region.regionGroup(getModule());
        return group.getSetting(setting) == checkAgainst;
    }

    public boolean checkSettingAt(final Block block, final EnvironmentSetting setting, final boolean checkAgainst) {
        final var region = getModule().regionAt(block);
        if (region == null) {
            return false;
        }

        final var group = region.regionGroup(getModule());
        return group.getSetting(setting) == checkAgainst;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent event) {
        // Prevent explosions from removing region blocks
        event.blockList().removeIf(block -> checkSettingAt(block, EnvironmentSetting.EXPLOSIONS, false));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        // Prevent explosions from removing region blocks
        event.blockList().removeIf(block -> checkSettingAt(block, EnvironmentSetting.EXPLOSIONS, false));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        // Prevent monster entities from changing region blocks
        if (checkSettingAt(event.getBlock(), EnvironmentSetting.MONSTERS, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBurn(final BlockBurnEvent event) {
        if (checkSettingAt(event.getBlock(), EnvironmentSetting.FIRE, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockSpread(final BlockSpreadEvent event) {
        EnvironmentSetting setting;
        switch (event.getNewState().getType()) {
            default:
                return;
            case FIRE:
                setting = EnvironmentSetting.FIRE;
                break;
            case VINE:
                setting = EnvironmentSetting.VINE_GROWTH;
                break;
        }

        if (checkSettingAt(event.getBlock(), setting, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCreatureSpawn(final CreatureSpawnEvent event) {
        // Only cancel natural spawns and alike
        switch (event.getSpawnReason()) {
            case JOCKEY:
            case MOUNT:
            case NATURAL:
                break;
            default:
                return;
        }

        final var entity = event.getEntity();
        if (entity instanceof Monster) {
            if (checkSettingAt(event.getLocation(), EnvironmentSetting.MONSTERS, false)) {
                event.setCancelled(true);
            }
        } else if (entity instanceof Animals) {
            if (checkSettingAt(event.getLocation(), EnvironmentSetting.ANIMALS, false)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        final var damaged = event.getEntity();
        final var damager = event.getDamager();

        if (damaged.getType() != EntityType.PLAYER) {
            return;
        }

        final Player playerDamaged = (Player) damaged;
        final Player playerDamager;
        if (damager instanceof Player) {
            playerDamager = (Player) damager;
        } else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
            playerDamager = (Player) ((Projectile) damager).getShooter();
        } else {
            return;
        }

        if (
            playerDamager != null &&
            playerDamaged != playerDamager &&
            (checkSettingAt(playerDamaged.getLocation(), EnvironmentSetting.PVP, false) ||
                checkSettingAt(playerDamager.getLocation(), EnvironmentSetting.PVP, false))
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingBreakEvent(final HangingBreakEvent event) {
        switch (event.getCause()) {
            default:
                return;
            case ENTITY:
                return; // Handeled by onHangingBreakByEntity
            case EXPLOSION: {
                if (checkSettingAt(event.getEntity().getLocation(), EnvironmentSetting.EXPLOSIONS, false)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }

        final var block = event.getClickedBlock();
        if (block != null && block.getType() == Material.FARMLAND) {
            if (checkSettingAt(block, EnvironmentSetting.TRAMPLE, false)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPotionSplash(final PotionSplashEvent event) {
        // Only if a player threw the potion check for PVP
        if (!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }

        final var thrower = (Player) event.getEntity().getShooter();
        final var sourcePvpRestricted = checkSettingAt(thrower.getLocation(), EnvironmentSetting.PVP, false);

        // Cancel all damage to players if either thrower or damaged is
        // inside no-PVP region
        for (final var target : event.getAffectedEntities()) {
            if (!(target instanceof Player)) {
                continue;
            }

            if (sourcePvpRestricted || checkSettingAt(target.getLocation(), EnvironmentSetting.PVP, false)) {
                event.setIntensity(target, 0);
                return;
            }
        }
    }
}
