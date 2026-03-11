package org.oddlama.vane.util

import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*

class LazyLocation(
    val worldId: UUID?,
    private val location: Location,
) {
    constructor(location: Location) : this(
        worldId = location.world?.uid,
        location = location.clone(),
    )

    constructor(worldId: UUID?, x: Double, y: Double, z: Double, pitch: Float, yaw: Float) : this(
        worldId = worldId,
        location = Location(null, x, y, z, pitch, yaw),
    )

    fun location(): Location {
        if (worldId != null && location.world == null) {
            location.world = Bukkit.getWorld(worldId)
        }
        return location
    }
}
