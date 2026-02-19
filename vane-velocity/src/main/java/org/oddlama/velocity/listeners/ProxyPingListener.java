package org.oddlama.velocity.listeners;

import static org.oddlama.velocity.Util.getServerForHost;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import org.oddlama.vane.proxycore.listeners.PingEvent;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.VelocityCompatServerInfo;
import org.oddlama.velocity.compat.event.VelocityCompatPingEvent;

public class ProxyPingListener {

    final Velocity velocity;

    public ProxyPingListener(Velocity velocity) {
        this.velocity = velocity;
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ProxyServer proxy = velocity.getRawProxy();

        final var virtualHost = event.getConnection().getVirtualHost();
        if (virtualHost.isEmpty()) return;

        final var server = getServerForHost(proxy, virtualHost.get());

        var serverInfo = new VelocityCompatServerInfo(server);
        PingEvent proxyEvent = new VelocityCompatPingEvent(velocity, event, serverInfo);
        proxyEvent.fire();
    }
}
