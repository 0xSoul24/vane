package org.oddlama.vane.proxycore.listeners

interface ProxyCancellableEvent {
    fun cancel()

    fun cancel(reason: String?)
}
