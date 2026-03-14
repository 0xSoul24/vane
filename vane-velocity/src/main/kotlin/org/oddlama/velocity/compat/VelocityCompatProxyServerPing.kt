package org.oddlama.velocity.compat

import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.util.Favicon
import net.kyori.adventure.text.Component
import org.oddlama.vane.proxycore.listeners.ProxyServerPing

/**
 * Mutable proxy-core ping adapter backed by a Velocity ping builder.
 *
 * @param ping source ping snapshot to mutate.
 */
class VelocityCompatProxyServerPing(ping: ServerPing) : ProxyServerPing {
    /**
     * Velocity builder used to assemble the outgoing ping response.
     */
    val builder: ServerPing.Builder = ping.asBuilder()

    /**
     * Updates the MOTD text in the ping response.
     *
     * @param description new plain-text description.
     */
    override fun setDescription(description: String?) {
        builder.description(Component.text(description.orEmpty()))
    }

    /**
     * Updates the favicon in the ping response when provided.
     *
     * @param encodedFavicon base64-encoded favicon string.
     */
    override fun setFavicon(encodedFavicon: String?) {
        encodedFavicon?.let { builder.favicon(Favicon(it)) }
    }
}
