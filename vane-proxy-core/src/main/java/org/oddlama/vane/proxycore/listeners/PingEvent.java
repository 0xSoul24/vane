package org.oddlama.vane.proxycore.listeners;

import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;

public abstract class PingEvent implements ProxyEvent {

    public VaneProxyPlugin plugin;
    public ProxyServerPing ping;
    public IVaneProxyServerInfo server;

    public PingEvent(VaneProxyPlugin plugin, ProxyServerPing ping, IVaneProxyServerInfo server) {
        this.plugin = plugin;
        this.ping = ping;
        this.server = server;
    }

    public void fire() {
        ping.setDescription(plugin.getMotd(server));
        ping.setFavicon(plugin.getFavicon(server));

        this.sendResponse();
    }

    public abstract void sendResponse();
}
