package org.oddlama.vane.proxycore.commands

import org.oddlama.vane.proxycore.ProxyPlayer
import org.oddlama.vane.proxycore.VaneProxyPlugin

class ProxyPingCommand(permission: String?, plugin: VaneProxyPlugin) : ProxyCommand(permission, plugin) {
    override fun execute(sender: ProxyCommandSender?, args: Array<String?>?) {
        if (sender !is ProxyPlayer) {
            sender?.sendMessage("Not a player!")
            return
        }

        if (!hasPermission(sender.uniqueId)) {
            sender.sendMessage("No permission!")
            return
        }

        sender.sendMessage("§7ping: §3" + sender.ping + "ms")
    }
}
