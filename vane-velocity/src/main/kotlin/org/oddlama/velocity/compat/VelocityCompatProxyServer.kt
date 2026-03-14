package org.oddlama.velocity.compat

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import org.oddlama.vane.proxycore.ProxyPlayer
import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler
import org.oddlama.velocity.compat.scheduler.VelocityCompatProxyTaskScheduler
import java.util.UUID

/**
 * Velocity implementation of the proxy-core server abstraction.
 *
 * @property proxy wrapped Velocity proxy server.
 */
class VelocityCompatProxyServer(private val proxy: ProxyServer) : org.oddlama.vane.proxycore.ProxyServer {
    /**
     * Scheduler adapter exposing Velocity scheduling through proxy-core interfaces.
     */
    override val scheduler: ProxyTaskScheduler
        get() = VelocityCompatProxyTaskScheduler(proxy.scheduler)

    /**
     * Broadcasts a plain-text message to all connected players.
     *
     * @param message message to broadcast.
     */
    override fun broadcast(message: String?) {
        proxy.sendMessage(Component.text(message.orEmpty()))
    }

    /**
     * Snapshot of currently connected players wrapped as proxy-core players.
     */
    override val players: MutableCollection<ProxyPlayer?>
        get() = proxy.allPlayers.mapTo(mutableListOf()) { VelocityCompatProxyPlayer(it) as ProxyPlayer? }

    /**
     * Checks whether a specific player has any of the given permissions.
     *
     * @param uuid target player UUID.
     * @param permission candidate permissions to evaluate.
     * @return `true` when the player exists and has at least one permission.
     */
    override fun hasPermission(uuid: UUID?, vararg permission: String?): Boolean {
        if (uuid == null) return false
        return proxy.getPlayer(uuid)
            .map { player -> permission.filterNotNull().any(player::hasPermission) }
            .orElse(false)
    }
}
