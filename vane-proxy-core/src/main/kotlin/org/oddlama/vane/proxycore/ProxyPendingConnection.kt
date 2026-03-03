package org.oddlama.vane.proxycore

import java.net.SocketAddress
import java.util.UUID

interface ProxyPendingConnection {
    val name: String?

    val uniqueId: UUID?

    val port: Int

    val socketAddress: SocketAddress?

    fun hasPermission(server: ProxyServer?, vararg permission: String?): Boolean

    fun canStartServer(server: ProxyServer?, serverName: String?): Boolean {
        return hasPermission(
            server,
            "vane_proxy.start_server",
            "vane_proxy.start_server.*",
            "vane_proxy.start_server.$serverName"
        )
    }
}
