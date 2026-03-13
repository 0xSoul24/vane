package org.oddlama.vane.proxycore

import org.oddlama.vane.proxycore.commands.ProxyCommandSender
import java.util.UUID

/**
 * Represents a connected player on the proxy.
 */
interface ProxyPlayer : ProxyCommandSender {
    /**
     * Disconnects the player with a reason message.
     *
     * @param message formatted disconnect reason.
     */
    fun disconnect(message: String?)

    /** Stable unique identifier for the player. */
    val uniqueId: UUID?

    /** Current network latency in milliseconds. */
    val ping: Long
}
