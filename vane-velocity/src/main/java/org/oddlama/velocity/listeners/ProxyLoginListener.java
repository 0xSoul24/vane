package org.oddlama.velocity.listeners;

import static org.oddlama.velocity.Util.getServerForHost;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.ProxyServer;
import org.oddlama.vane.proxycore.listeners.LoginEvent;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.VelocityCompatServerInfo;
import org.oddlama.velocity.compat.event.VelocityCompatLoginEvent;
import org.oddlama.velocity.compat.event.VelocityCompatPendingConnection;

public class ProxyLoginListener {

    final Velocity velocity;

    @Inject
    public ProxyLoginListener(Velocity velocity) {
        this.velocity = velocity;
    }

    @Subscribe(priority = 0)
    public void login(com.velocitypowered.api.event.connection.LoginEvent event) {
        if (!event.getResult().isAllowed()) return;

        ProxyServer proxy = velocity.getRawProxy();

        final var virtualHost = event.getPlayer().getVirtualHost();
        if (virtualHost.isEmpty()) return;

        final var server = getServerForHost(proxy, virtualHost.get());

        var serverInfo = new VelocityCompatServerInfo(server);
        LoginEvent proxyEvent = new VelocityCompatLoginEvent(
            event,
            velocity,
            serverInfo,
            new VelocityCompatPendingConnection(event.getPlayer())
        );
        proxyEvent.fire();
    }
}
