package org.oddlama.velocity.commands

import com.velocitypowered.api.command.SimpleCommand
import org.oddlama.vane.proxycore.commands.ProxyMaintenanceCommand
import org.oddlama.velocity.Velocity
import org.oddlama.velocity.compat.VelocityCompatProxyCommandSender

class Maintenance(plugin: Velocity) : SimpleCommand {
    var cmd: ProxyMaintenanceCommand = ProxyMaintenanceCommand("vane_proxy.commands.maintenance", plugin)

    override fun execute(invocation: SimpleCommand.Invocation) {
        cmd.execute(VelocityCompatProxyCommandSender(invocation.source()), invocation.arguments())
    }
}
