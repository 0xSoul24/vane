package org.oddlama.velocity.compat.event;

import java.util.logging.Level;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;

public class VelocityCompatPreLoginEvent extends PreLoginEvent {

    final com.velocitypowered.api.event.connection.PreLoginEvent event;

    public VelocityCompatPreLoginEvent(
        VaneProxyPlugin plugin,
        com.velocitypowered.api.event.connection.PreLoginEvent event
    ) {
        super(plugin);
        this.event = event;
    }

    @Override
    public void cancel() {
        cancel("");
    }

    @Override
    public void cancel(String reason) {
        plugin
            .getLogger()
            .log(
                Level.WARNING,
                "Denying multiplexer connection from " +
                event.getConnection().getRemoteAddress() +
                ": " +
                (reason.isEmpty() ? "No reason provided" : reason)
            );

        event.setResult(
            com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult.denied(
                LegacyComponentSerializer.legacySection()
                    .deserialize(reason.isEmpty() ? "Failed to authorize multiplexer connection" : reason)
            )
        );
    }

    @Override
    public ProxyPendingConnection getConnection() {
        return new VelocityCompatPendingConnection(event.getConnection(), event.getUsername());
    }

    @Override
    public boolean implementationSpecificAuth(MultiplexedPlayer multiplexedPlayer) {
        // Not applicable, all handled in `ProxyGameProfileRequestListener`
        return true;
    }
}
