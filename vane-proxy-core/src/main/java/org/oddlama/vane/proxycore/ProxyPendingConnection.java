package org.oddlama.vane.proxycore;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface ProxyPendingConnection {
    String getName();

    @Nullable
    UUID getUniqueId();

    int getPort();

    java.net.SocketAddress getSocketAddress();

    boolean hasPermission(ProxyServer server, final String... permission);

    default boolean canStartServer(ProxyServer server, String serverName) {
        return hasPermission(
            server,
            "vane_proxy.start_server",
            "vane_proxy.start_server.*",
            "vane_proxy.start_server." + serverName
        );
    }
}
