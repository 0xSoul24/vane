package org.oddlama.vane.proxycore.listeners

import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo

abstract class PingEvent(var plugin: VaneProxyPlugin, @JvmField var ping: ProxyServerPing, var server: IVaneProxyServerInfo) :
    ProxyEvent {
    override fun fire() {
        ping.setDescription(plugin.getMotd(server))
        ping.setFavicon(plugin.getFavicon(server))

        this.sendResponse()
    }

    abstract fun sendResponse()
}
