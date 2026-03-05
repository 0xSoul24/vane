package org.oddlama.velocity

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import java.net.InetSocketAddress

object Util {
    @JvmStatic
    fun getServerForHost(proxy: ProxyServer, host: InetSocketAddress): RegisteredServer {
        val forcedHosts = proxy.configuration.forcedHosts

        val forced: String
        var server: RegisteredServer
        try {
            forced = forcedHosts[host.hostString]!![0]
            if (forced.isEmpty()) throw Exception()
            server = proxy.getServer(forced).get()
        } catch (ignored: Exception) {
            server = proxy.getServer(proxy.configuration.attemptConnectionOrder[0]).get()
        }

        return server
    }
}
