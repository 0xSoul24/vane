package org.oddlama.vane.proxycore

import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler
import java.util.*

interface ProxyServer {
    val scheduler: ProxyTaskScheduler?

    fun broadcast(message: String?)

    val players: MutableCollection<ProxyPlayer?>?

    fun hasPermission(uuid: UUID?, vararg permission: String?): Boolean
}
