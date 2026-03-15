package org.oddlama.vane.portals.event

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Base class for cancellable portal events. */
abstract class PortalEvent : Event(), Cancellable {
    /** Stores the cancellation state. */
    private var cancelled = false

    /** Returns Bukkit handlers for the concrete portal event. */
    abstract override fun getHandlers(): HandlerList

    /** Returns whether this event is cancelled. */
    override fun isCancelled() = cancelled

    /** Sets the cancellation state for this event. */
    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }
}
