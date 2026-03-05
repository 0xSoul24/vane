package org.oddlama.velocity.compat

import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.util.Favicon
import net.kyori.adventure.text.Component
import org.oddlama.vane.proxycore.listeners.ProxyServerPing

class VelocityCompatProxyServerPing(ping: ServerPing) : ProxyServerPing {
    val ping: ServerPing.Builder = ping.asBuilder()

    override fun setDescription(description: String?) {
        ping.description(Component.text(description.orEmpty()))
    }

    override fun setFavicon(encodedFavicon: String?) {
        if (encodedFavicon != null) ping.favicon(Favicon(encodedFavicon))
    }
}
