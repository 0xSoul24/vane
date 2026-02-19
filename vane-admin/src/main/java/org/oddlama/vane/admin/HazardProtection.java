package org.oddlama.vane.admin;

import java.util.List;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigStringList;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

public class HazardProtection extends Listener<Admin> {

    private WorldRebuild worldRebuild;

    @ConfigBoolean(def = true, desc = "Restrict wither spawning to a list of worlds defined by wither_world_whitelist.")
    private boolean configEnableWitherWorldWhitelist;

    @ConfigStringList(
        def = { "world_nether", "world_the_end" },
        desc = "A list of worlds in which the wither may be spawned."
    )
    private List<String> configWitherWorldWhitelist;

    @ConfigBoolean(def = true, desc = "Disables explosions from the wither.")
    private boolean configDisableWitherExplosions;

    @ConfigBoolean(def = true, desc = "Disables explosions from creepers.")
    private boolean configDisableCreeperExplosions;

    @ConfigBoolean(def = true, desc = "Disables enderman block pickup.")
    private boolean configDisableEndermanBlockPickup;

    @ConfigBoolean(def = true, desc = "Disables entities from breaking doors (various zombies).")
    private boolean configDisableDoorBreaking;

    @ConfigBoolean(def = true, desc = "Disables fire from lightning.")
    private boolean configDisableLightningFire;

    @LangMessage
    private TranslatedMessage langWitherSpawnProhibited;

    public HazardProtection(Context<Admin> context) {
        super(
            context.group(
                "HazardProtection",
                "Enable hazard protection. The options below allow more fine-grained control over the hazards to protect from."
            )
        );
        worldRebuild = new WorldRebuild(getContext());
    }

    private boolean disableExplosion(EntityType type) {
        switch (type) {
            default:
                return false;
            case WITHER:
            case WITHER_SKULL:
                return configDisableWitherExplosions;
            case CREEPER:
                return configDisableCreeperExplosions;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        if (disableExplosion(event.getEntityType())) {
            if (worldRebuild.enabled()) {
                // Schedule rebuild
                worldRebuild.rebuild(event.blockList());
                // Remove all affected blocks from event
                event.blockList().clear();
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreakByEntity(final HangingBreakByEntityEvent event) {
        if (disableExplosion(event.getRemover().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityBreakDoor(final EntityBreakDoorEvent event) {
        if (configDisableDoorBreaking) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(final BlockIgniteEvent event) {
        if (event.getCause() == BlockIgniteEvent.IgniteCause.LIGHTNING && configDisableLightningFire) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(final CreatureSpawnEvent event) {
        if (!configEnableWitherWorldWhitelist) {
            return;
        }

        // Only for wither spawns
        if (event.getEntity().getType() != EntityType.WITHER) {
            return;
        }

        // Check if the world is whitelisted
        final var world = event.getEntity().getWorld();
        if (configWitherWorldWhitelist.contains(world.getName())) {
            return;
        }

        langWitherSpawnProhibited.broadcastWorld(world, world.getName());
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityBlockChange(final EntityChangeBlockEvent event) {
        if (!configDisableEndermanBlockPickup) {
            return;
        }

        // Only for enderman events
        if (event.getEntity().getType() != EntityType.ENDERMAN) {
            return;
        }

        event.setCancelled(true);
    }
}
