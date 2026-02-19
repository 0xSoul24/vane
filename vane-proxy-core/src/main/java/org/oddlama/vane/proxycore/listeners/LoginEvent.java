package org.oddlama.vane.proxycore.listeners;

import java.util.logging.Level;
import org.oddlama.vane.proxycore.Maintenance;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;

public abstract class LoginEvent implements ProxyEvent, ProxyCancellableEvent {

    VaneProxyPlugin plugin;
    IVaneProxyServerInfo serverInfo;
    ProxyPendingConnection connection;

    public LoginEvent(VaneProxyPlugin plugin, IVaneProxyServerInfo serverInfo, ProxyPendingConnection connection) {
        this.plugin = plugin;
        this.serverInfo = serverInfo;
        this.connection = connection;
    }

    public final void fire() {
        final var connectionUuid = connection.getUniqueId();

        // We're in the LoginEvent, the UUID should be resolved
        assert connectionUuid != null;

        final var uuid = plugin.getMultiplexedUuids().getOrDefault(connectionUuid, connectionUuid);

        if (!plugin.canJoinMaintenance(uuid)) {
            this.cancel(plugin.getMaintenance().formatMessage(Maintenance.MESSAGE_CONNECT));
            return;
        }

        plugin
            .getLogger()
            .log(
                Level.INFO,
                "Connection '" + connection.getName() + "' is connecting to '" + serverInfo.getName() + "'"
            );

        // Start server if necessary
        if (!plugin.isOnline(serverInfo)) {
            // For use inside callback
            final var cms = plugin.getConfig().managedServers.get(serverInfo.getName());

            if (!cms.start.allowAnyone && !connection.canStartServer(plugin.getProxy(), serverInfo.getName())) {
                plugin
                    .getLogger()
                    .log(
                        Level.INFO,
                        "Disconnecting '" +
                        connection.getName() +
                        "' because they don't have the permission to start server '" +
                        serverInfo.getName() +
                        "'"
                    );
                // TODO: This could probably use a configurable message?
                this.cancel("Server is offline and you don't have the permission to start it");
                return;
            }

            if (cms == null || cms.startCmd() == null) {
                plugin
                    .getLogger()
                    .log(
                        Level.SEVERE,
                        "Could not start server '" + serverInfo.getName() + "', no start command was set!"
                    );
                this.cancel("Could not start server");
            } else {
                // Client is connecting while startup
                plugin.tryStartServer(cms);

                if (cms.startKickMsg() == null) {
                    this.cancel("Server is starting");
                } else {
                    this.cancel(cms.startKickMsg());
                }
            }
        }
    }
}
