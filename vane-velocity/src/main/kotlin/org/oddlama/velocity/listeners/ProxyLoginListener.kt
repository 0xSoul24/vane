package org.oddlama.velocity.listeners

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import org.oddlama.velocity.Util.getServerForHost
import org.oddlama.velocity.Velocity
import org.oddlama.velocity.compat.VelocityCompatServerInfo
import org.oddlama.velocity.compat.event.VelocityCompatLoginEvent
import org.oddlama.velocity.compat.event.VelocityCompatPendingConnection

class ProxyLoginListener @Inject constructor(val velocity: Velocity) {
    @Subscribe(priority = 0)
    fun login(event: LoginEvent) {
        if (!event.result.isAllowed) return

        val proxy = velocity.rawProxy

        val virtualHost = event.player.virtualHost
        if (virtualHost.isEmpty) return

        val server = getServerForHost(proxy, virtualHost.get())

        val serverInfo = VelocityCompatServerInfo(server)
        val proxyEvent: org.oddlama.vane.proxycore.listeners.LoginEvent = VelocityCompatLoginEvent(
            event,
            velocity,
            serverInfo,
            VelocityCompatPendingConnection(event.player)
        )
        proxyEvent.fire()
    }
}
