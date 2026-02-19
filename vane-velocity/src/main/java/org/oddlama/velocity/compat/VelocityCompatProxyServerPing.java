package org.oddlama.velocity.compat;

import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import net.kyori.adventure.text.Component;
import org.oddlama.vane.proxycore.listeners.ProxyServerPing;

public class VelocityCompatProxyServerPing implements ProxyServerPing {

    public final ServerPing.Builder ping;

    public VelocityCompatProxyServerPing(ServerPing ping) {
        this.ping = ping.asBuilder();
    }

    @Override
    public void setDescription(String description) {
        ping.description(Component.text(description));
    }

    @Override
    public void setFavicon(String encodedFavicon) {
        if (encodedFavicon != null) ping.favicon(new Favicon(encodedFavicon));
    }
}
