package org.oddlama.velocity.listeners

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import org.oddlama.velocity.Velocity

/**
 * Cleans up multiplexed UUID mappings when players disconnect.
 *
 * @property velocity plugin instance that stores multiplexed UUID mappings.
 */
class ProxyDisconnectListener @Inject constructor(private val velocity: Velocity) {
    /**
     * Removes the disconnected player's multiplex mapping.
     *
     * @param event Velocity disconnect event.
     */
    @Subscribe(priority = 0)
    fun disconnect(event: DisconnectEvent) {
        val uuid = event.player.uniqueId
        velocity.multiplexedUuids.remove(uuid)
    }
}
