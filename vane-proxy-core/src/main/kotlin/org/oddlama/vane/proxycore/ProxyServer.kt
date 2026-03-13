package org.oddlama.vane.proxycore

import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler
import java.util.UUID

/**
 * Abstraction of a proxy runtime used by [VaneProxyPlugin].
 */
interface ProxyServer {
    /** Scheduler used for asynchronous and delayed tasks. */
    val scheduler: ProxyTaskScheduler?

    /**
     * Broadcasts a message to all connected players.
     *
     * @param message formatted message to broadcast.
     */
    fun broadcast(message: String?)

    /** Collection of currently connected players. */
    val players: MutableCollection<ProxyPlayer?>?

    /**
     * Checks whether the subject represented by [uuid] has at least one of the provided permissions.
     *
     * @param uuid player UUID to check.
     * @param permission permissions to test.
     * @return `true` when permission checks pass.
     */
    fun hasPermission(uuid: UUID?, vararg permission: String?): Boolean
}
