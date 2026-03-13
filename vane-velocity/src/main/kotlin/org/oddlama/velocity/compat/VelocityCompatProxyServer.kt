package org.oddlama.velocity.compat

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import org.oddlama.vane.proxycore.ProxyPlayer
import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler
import org.oddlama.velocity.compat.scheduler.VelocityCompatProxyTaskScheduler
import java.util.*

class VelocityCompatProxyServer(val proxy: ProxyServer) : org.oddlama.vane.proxycore.ProxyServer {
    override val scheduler: ProxyTaskScheduler
        get() = VelocityCompatProxyTaskScheduler(proxy.scheduler)

    override fun broadcast(message: String?) {
        proxy.sendMessage(Component.text(message.orEmpty()))
    }

    override val players: MutableCollection<ProxyPlayer?>
        get() = proxy.allPlayers.map { VelocityCompatProxyPlayer(it) as ProxyPlayer? }.toMutableList()

    override fun hasPermission(uuid: UUID?, vararg permission: String?): Boolean {
        val player = proxy.getPlayer(uuid)
        if (player.isEmpty) return false

        return Arrays.stream<String?>(permission).anyMatch { perm: String? -> player.get().hasPermission(perm) }
    }
}
