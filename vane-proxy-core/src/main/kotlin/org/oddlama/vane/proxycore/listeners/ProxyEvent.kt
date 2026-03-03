package org.oddlama.vane.proxycore.listeners

import org.oddlama.vane.proxycore.ProxyPendingConnection

interface ProxyEvent {
    val connection: ProxyPendingConnection?

    fun fire()
}
