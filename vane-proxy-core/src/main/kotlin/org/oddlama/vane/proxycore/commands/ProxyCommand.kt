package org.oddlama.vane.proxycore.commands

import org.oddlama.vane.proxycore.VaneProxyPlugin
import java.util.*

abstract class ProxyCommand(val permission: String?, @JvmField val plugin: VaneProxyPlugin) {
    abstract fun execute(sender: ProxyCommandSender?, args: Array<String?>?)

    fun hasPermission(uuid: UUID?): Boolean {
        return this.permission == null || plugin.proxy.hasPermission(uuid, this.permission)
    }
}
