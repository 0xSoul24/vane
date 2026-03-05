package org.oddlama.velocity.listeners

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import org.oddlama.velocity.Velocity

class ProxyDisconnectListener @Inject constructor(val velocity: Velocity) {
    @Subscribe(priority = 0)
    fun disconnect(event: DisconnectEvent) {
        val uuid = event.player.uniqueId
        velocity.multiplexedUuids.remove(uuid)
    }
}
