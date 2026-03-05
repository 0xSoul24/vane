package org.oddlama.velocity.compat

import com.velocitypowered.api.proxy.server.RegisteredServer
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo
import org.oddlama.velocity.Velocity
import java.net.SocketAddress

class VelocityCompatServerInfo(val server: RegisteredServer) : IVaneProxyServerInfo {
    override val name: String?
        get() = server.serverInfo.name

    override val socketAddress: SocketAddress?
        get() = server.serverInfo.address

    override fun sendData(data: ByteArray?) {
        if (data == null) return
        server.sendPluginMessage(Velocity.CHANNEL, data)
    }

    override fun sendData(data: ByteArray?, queue: Boolean): Boolean {
        // Not applicable
        assert(false)
        return false
    }
}
