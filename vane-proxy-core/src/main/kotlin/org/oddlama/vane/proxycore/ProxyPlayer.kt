package org.oddlama.vane.proxycore

import org.oddlama.vane.proxycore.commands.ProxyCommandSender
import java.util.UUID

interface ProxyPlayer : ProxyCommandSender {
    fun disconnect(message: String?)

    val uniqueId: UUID?

    val ping: Long
}
