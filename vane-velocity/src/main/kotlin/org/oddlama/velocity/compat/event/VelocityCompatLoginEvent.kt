package org.oddlama.velocity.compat.event

import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.connection.LoginEvent
import net.kyori.adventure.text.Component
import org.oddlama.vane.proxycore.ProxyPendingConnection
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo

class VelocityCompatLoginEvent(
    val event: LoginEvent,
    plugin: VaneProxyPlugin,
    serverInfo: IVaneProxyServerInfo,
    override var connection: ProxyPendingConnection
) : org.oddlama.vane.proxycore.listeners.LoginEvent(plugin, serverInfo, connection) {
    override fun cancel() {
        cancel(null)
    }

    // Implement the nullable signature expected by the base class
    override fun cancel(reason: String?) {
        val text = reason ?: ""
        event.result = ResultedEvent.ComponentResult.denied(Component.text(text))
    }
}
