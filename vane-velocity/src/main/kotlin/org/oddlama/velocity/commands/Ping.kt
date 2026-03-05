package org.oddlama.velocity.commands

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import org.oddlama.vane.proxycore.commands.ProxyPingCommand
import org.oddlama.velocity.Velocity
import org.oddlama.velocity.compat.VelocityCompatProxyCommandSender
import org.oddlama.velocity.compat.VelocityCompatProxyPlayer

class Ping(plugin: Velocity) : SimpleCommand {
    var cmd: ProxyPingCommand = ProxyPingCommand("vane_proxy.commands.ping", plugin)

    override fun execute(invocation: SimpleCommand.Invocation) {
        val sender = invocation.source()
        cmd.execute(
            if (sender is Player)
                VelocityCompatProxyPlayer(sender)
            else
                VelocityCompatProxyCommandSender(sender),
            invocation.arguments()
        )
    }
}
