package org.oddlama.velocity.compat.event

import com.velocitypowered.api.event.connection.PreLoginEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.oddlama.vane.proxycore.ProxyPendingConnection
import org.oddlama.vane.proxycore.VaneProxyPlugin
import java.util.logging.Level

class VelocityCompatPreLoginEvent(
    plugin: VaneProxyPlugin,
    val event: PreLoginEvent
) : org.oddlama.vane.proxycore.listeners.PreLoginEvent(plugin) {
    override fun cancel() {
        cancel(null)
    }

    override fun cancel(reason: String?) {
        val r = reason.orEmpty()

        plugin
            .getLogger()
            .log(
                Level.WARNING,
                "Denying multiplexer connection from " +
                        event.connection.remoteAddress +
                        ": " +
                        (r.ifEmpty { "No reason provided" })
            )

        event.result = PreLoginEvent.PreLoginComponentResult.denied(
            LegacyComponentSerializer.legacySection()
                .deserialize(r.ifEmpty { "Failed to authorize multiplexer connection" })
        )
    }

    override val connection: ProxyPendingConnection
        get() = VelocityCompatPendingConnection(event.connection, event.username)

    override fun implementationSpecificAuth(multiplexedPlayer: MultiplexedPlayer?): Boolean {
        // Not applicable, all handled in `ProxyGameProfileRequestListener`
        return true
    }
}
