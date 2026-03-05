package org.oddlama.velocity.compat.event

import com.velocitypowered.api.proxy.InboundConnection
import com.velocitypowered.api.proxy.Player
import org.oddlama.vane.proxycore.ProxyPendingConnection
import org.oddlama.vane.proxycore.ProxyServer
import java.net.SocketAddress
import java.util.*

class VelocityCompatPendingConnection : ProxyPendingConnection {
    val connection: InboundConnection
    override val name: String?
    override val uniqueId: UUID?

    constructor(connection: InboundConnection, username: String?) {
        this.connection = connection
        this.name = username
        this.uniqueId = null
    }

    constructor(player: Player) {
        this.connection = player
        this.name = player.username
        this.uniqueId = player.uniqueId
    }

    override val port: Int
        get() = connection.virtualHost.get().port

    override val socketAddress: SocketAddress
        get() = connection.virtualHost.get()

    override fun hasPermission(server: ProxyServer?, vararg permission: String?): Boolean {
        // Safe cast, we don't use this outside the case where this is constructed
        // with a Player
        return Arrays.stream<String?>(permission)
            .anyMatch { perm: String? -> (connection as Player).hasPermission(perm) }
    }
}
