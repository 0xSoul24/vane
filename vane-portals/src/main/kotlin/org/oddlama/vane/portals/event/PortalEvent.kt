package org.oddlama.vane.portals.event

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

abstract class PortalEvent : Event(), Cancellable {
    private var cancelled = false

    abstract override fun getHandlers(): HandlerList

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }
}
