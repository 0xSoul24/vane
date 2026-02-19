package org.oddlama.vane.portals;

import com.destroystokyo.paper.event.entity.EntityTeleportEndGatewayEvent;
import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.util.Vector;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.portals.event.EntityMoveEvent;
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.util.Nms;

public class PortalTeleporter extends Listener<Portals> {

    private final HashMap<UUID, Location> entitiesPortalling = new HashMap<>();

    public PortalTeleporter(Context<Portals> context) {
        super(context);
    }

    private boolean cancelPortalEvent(final Entity entity) {
        if (entitiesPortalling.containsKey(entity.getUniqueId())) {
            return true;
        }

        return getModule().isPortalBlock(entity.getLocation().getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(final PlayerPortalEvent event) {
        if (cancelPortalEvent(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTeleportEvent(final EntityTeleportEvent event) {
        if (cancelPortalEvent(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTeleportEndGatewayEvent(final EntityTeleportEndGatewayEvent event) {
        // End gateway teleport can be initiated when the bounding boxes overlap, so
        // the entity location will not necessarily be at the position where the end gateway block
        // is.
        // Therefore, we additionally check whether the initiating end gateway block is part of a
        // portal structure.
        // Otherwise, this event would already be handled by EntityTeleportEvent.
        if (getModule().isPortalBlock(event.getGateway().getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerTeleportEvent(final PlayerTeleportEndGatewayEvent event) {
        final var block = event.getGateway().getBlock();
        if (getModule().isPortalBlock(block)) {
            event.setCancelled(true);
        }
    }

    private void teleportSingleEntity(
        final Entity entity,
        final Location targetLocation,
        final Vector newVelocity
    ) {
        final var entityLocation = entity.getLocation();
        final var nmsEntity = Nms.entityHandle(entity);

        // Now teleport. There are many ways to do it, but some are preferred over others.
        // Primarily, entity.teleport() will dismount any passengers. Meh.
        if (targetLocation.getWorld() == entityLocation.getWorld()) {
            if (entity instanceof Player player) {
                // For players traveling in the same world, we can use the NMS player's connection's
                // teleport method, which only modifies player position without dismounting.
                Nms.getPlayer(player).connection.teleport(
                    targetLocation.getX(),
                    targetLocation.getY(),
                    targetLocation.getZ(),
                    targetLocation.getYaw(),
                    targetLocation.getPitch()
                );
            } else {
                // Similarly, we can just move entities.
                nmsEntity.absSnapTo(
                    targetLocation.getX(),
                    targetLocation.getY(),
                    targetLocation.getZ(),
                    targetLocation.getYaw(),
                    targetLocation.getPitch()
                );
            }

            // For some unknown reason (SPIGOT-619) we always need to set the yaw again.
            nmsEntity.setYHeadRot(targetLocation.getYaw());
        } else {
            final var passengers = new ArrayList<Entity>(entity.getPassengers());

            // Entities traveling to a different dimension need to be despawned and respawned as
            // both worlds are distinct levels.
            // This means they must be dismounted (or unridden) before teleportation.
            passengers.stream().forEach(entity::removePassenger);
            entity.teleport(targetLocation);

            for (var p : passengers) {
                teleportSingleEntity(p, targetLocation, new Vector());
                entity.addPassenger(p);
            }
        }

        // Retain velocity. Previously we needed to force-set it in the next tick,
        // as apparently the movement event overrides might override the velocity.
        // Now we are using our own movement events which are run outside any
        // entity ticking, so no such workaround is necessary.
        // scheduleNextTick(() -> {
        // entity.setVelocity(newVelocity);
        // });
        entity.setVelocity(newVelocity);
    }

    private void teleportEntity(final Entity entity, final Portal source, Portal target) {
        var targetLocation = target.spawn().clone();
        if (entity instanceof LivingEntity livingEntity) {
            // Entities in vehicles are teleported when the vehicle is teleported.
            if (livingEntity.isInsideVehicle()) {
                return;
            }

            // Increase Y value if an entity is currently flying through a portal that
            // has extent in the y direction (i.e., is built upright)
            if (livingEntity.isGliding() && source.orientation().plane().y()) {
                targetLocation.setY(targetLocation.getY() + 1.5);
            }
        }

        // Put null to signal initiated teleportation
        final var entityId = entity.getUniqueId();
        entitiesPortalling.put(entityId, null);

        // First copy pitch & yaw to target, will be transformed soon.
        final var entityLocation = entity.getLocation();
        targetLocation.setPitch(entityLocation.getPitch());
        targetLocation.setYaw(entityLocation.getYaw());

        // If the exit orientation of the target portal is locked, we make sure that
        // the orientation of the entered portal is flipped if an entity (player) enters from the
        // back.
        // We have to flip the source portal orientation if the vector to be transformed
        // is NOT opposing the source portal vector (i.e., not pointing against the front).
        // Calculate new location (pitch, yaw) and velocity.
        final var sourceOrientation = source.orientation();
        targetLocation = sourceOrientation.apply(
            target.orientation(),
            targetLocation,
            target.exitOrientationLocked()
        );
        final var newVelocity = sourceOrientation.apply(
            target.orientation(),
            entity.getVelocity(),
            target.exitOrientationLocked()
        );

        teleportSingleEntity(entity, targetLocation, newVelocity);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityMove(final EntityMoveEvent event) {
        final var entity = event.getEntity();
        final var entityId = entity.getUniqueId();
        final var block = event.getTo().getBlock();

        if (!entitiesPortalling.containsKey(entityId)) {
            // Check if we walked into a portal
            if (!getModule().portalAreaMaterials.contains(block.getType())) {
                return;
            }

            final var portal = getModule().portalFor(block);
            if (portal == null) {
                return;
            }

            final var target = getModule().connectedPortal(portal);
            if (target == null) {
                return;
            }

            teleportEntity(entity, portal, target);
        } else {
            final var loc = entitiesPortalling.get(entityId);
            if (loc == null) {
                // Initial teleport. Remember the current location, so we can check
                // that the entity moved away far enough to allow another teleport
                entitiesPortalling.put(entityId, event.getFrom().clone());
            } else if (!getModule().portalAreaMaterials.contains(block.getType())) {
                // At least 2 blocks away and outside of portal area → finish portalling.
                if (loc.getWorld() == event.getFrom().getWorld() && event.getFrom().distance(loc) > 2.0) {
                    entitiesPortalling.remove(entityId);
                }
            }
        }
    }
}
