package org.oddlama.velocity.compat

import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import org.oddlama.vane.proxycore.ProxyPlayer
import java.util.UUID

/**
 * Velocity implementation of the proxy-core player abstraction.
 *
 * @property player wrapped Velocity player.
 */
class VelocityCompatProxyPlayer(private val player: Player) : ProxyPlayer {
    /**
     * Disconnects the player with an optional plain-text message.
     *
     * @param message disconnect reason shown to the player.
     */
    override fun disconnect(message: String?) {
        player.disconnect(Component.text(message.orEmpty()))
    }

    /**
     * Unique identifier of the wrapped player.
     */
    override val uniqueId: UUID?
        get() = player.uniqueId

    /**
     * Current measured network latency in milliseconds.
     */
    override val ping: Long
        get() = player.ping

    /**
     * Sends a plain-text chat message to the player.
     *
     * @param message message to send.
     */
    override fun sendMessage(message: String?) {
        player.sendMessage(Component.text(message.orEmpty()))
    }
}
