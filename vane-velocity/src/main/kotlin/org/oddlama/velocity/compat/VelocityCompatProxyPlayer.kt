package org.oddlama.velocity.compat

import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import org.oddlama.vane.proxycore.ProxyPlayer
import java.util.UUID

class VelocityCompatProxyPlayer(val player: Player) : ProxyPlayer {
    override fun disconnect(message: String?) {
        player.disconnect(Component.text(message.orEmpty()))
    }

    override val uniqueId: UUID?
        get() = player.uniqueId

    override val ping: Long
        get() = player.ping

    override fun sendMessage(message: String?) {
        player.sendMessage(Component.text(message.orEmpty()))
    }
}
