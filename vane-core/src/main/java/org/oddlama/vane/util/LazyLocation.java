package org.oddlama.vane.util;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LazyLocation {

    private final UUID worldId;
    private Location location;

    public LazyLocation(final Location location) {
        this.worldId = location.getWorld() == null ? null : location.getWorld().getUID();
        this.location = location.clone();
    }

    public LazyLocation(final UUID worldId, double x, double y, double z, float pitch, float yaw) {
        this.worldId = worldId;
        this.location = new Location(null, x, y, z, pitch, yaw);
    }

    public UUID worldId() {
        return worldId;
    }

    public Location location() {
        if (worldId != null && location.getWorld() == null) {
            location.setWorld(Bukkit.getWorld(worldId));
        }

        return location;
    }
}
