package org.oddlama.velocity.compat

import com.velocitypowered.api.command.CommandSource
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.oddlama.vane.proxycore.commands.ProxyCommandSender

/**
 * Velocity implementation of proxy-core command sender abstraction.
 *
 * @property sender wrapped Velocity command source.
 */
class VelocityCompatProxyCommandSender(private val sender: CommandSource) : ProxyCommandSender {
    /**
     * Sends a legacy-section-formatted message to the command source.
     *
     * @param message message text using section color codes.
     */
    override fun sendMessage(message: String?) {
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message.orEmpty()))
    }
}
