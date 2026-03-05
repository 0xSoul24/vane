package org.oddlama.velocity.compat.event

import com.velocitypowered.api.event.proxy.ProxyPingEvent
import org.oddlama.vane.proxycore.ProxyPendingConnection
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo
import org.oddlama.vane.proxycore.listeners.PingEvent
import org.oddlama.velocity.compat.VelocityCompatProxyServerPing

class VelocityCompatPingEvent(plugin: VaneProxyPlugin, val event: ProxyPingEvent, server: IVaneProxyServerInfo) :
    PingEvent(
        plugin, VelocityCompatProxyServerPing(
            event.ping
        ), server
    ) {
    override fun sendResponse() {
        event.ping = (ping as VelocityCompatProxyServerPing).ping.build()
    }

    override val connection: ProxyPendingConnection?
        get() = null
}
