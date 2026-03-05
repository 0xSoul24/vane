package org.oddlama.velocity.listeners

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import org.oddlama.vane.proxycore.listeners.PingEvent
import org.oddlama.velocity.Util.getServerForHost
import org.oddlama.velocity.Velocity
import org.oddlama.velocity.compat.VelocityCompatServerInfo
import org.oddlama.velocity.compat.event.VelocityCompatPingEvent

class ProxyPingListener(val velocity: Velocity) {
    @Subscribe
    fun onProxyPing(event: ProxyPingEvent) {
        val proxy = velocity.rawProxy

        val virtualHost = event.connection.virtualHost
        if (virtualHost.isEmpty) return

        val server = getServerForHost(proxy, virtualHost.get())

        val serverInfo = VelocityCompatServerInfo(server)
        val proxyEvent: PingEvent = VelocityCompatPingEvent(velocity, event, serverInfo)
        proxyEvent.fire()
    }
}
