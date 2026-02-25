package org.oddlama.vane.util

import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*

class LazyLocation {
    private val worldId: UUID?
    private val location: Location

    constructor(location: Location) {
        this.worldId = if (location.getWorld() == null) null else location.getWorld().uid
        this.location = location.clone()
    }

    constructor(worldId: UUID?, x: Double, y: Double, z: Double, pitch: Float, yaw: Float) {
        this.worldId = worldId
        this.location = Location(null, x, y, z, pitch, yaw)
    }

    fun worldId(): UUID? {
        return worldId
    }

    fun location(): Location {
        if (worldId != null && location.getWorld() == null) {
            location.setWorld(Bukkit.getWorld(worldId))
        }

        return location
    }
}
