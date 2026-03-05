package org.oddlama.velocity.compat

import com.velocitypowered.api.command.CommandSource
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.oddlama.vane.proxycore.commands.ProxyCommandSender

class VelocityCompatProxyCommandSender(var sender: CommandSource) : ProxyCommandSender {
    override fun sendMessage(message: String?) {
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message.orEmpty()))
    }
}
