package org.oddlama.velocity.listeners

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.velocity.compat.event.VelocityCompatPreLoginEvent

class ProxyPreLoginListener @Inject constructor(val plugin: VaneProxyPlugin?) {
    @Subscribe
    fun preLogin(event: PreLoginEvent?) {
        if (plugin == null || event == null) return

        val proxyEvent: org.oddlama.vane.proxycore.listeners.PreLoginEvent =
            VelocityCompatPreLoginEvent(plugin, event)

        // For Velocity, our multiplexer connections need more work; they
        // later get handled in `ProxyGameProfileRequestListener`
        proxyEvent.fire(org.oddlama.vane.proxycore.listeners.PreLoginEvent.PreLoginDestination.PENDING_MULTIPLEXED_LOGINS)
    }
}
