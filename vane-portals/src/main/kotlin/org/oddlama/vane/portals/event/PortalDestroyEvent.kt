package org.oddlama.vane.portals.event

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.oddlama.vane.portals.portal.Portal

class PortalDestroyEvent(@JvmField val player: Player, @JvmField val portal: Portal, private val checkOnly: Boolean) : PortalEvent() {
    private var cancelIfNotOwner = true

    fun setCancelIfNotOwner(cancelIfNotOwner: Boolean) {
        this.cancelIfNotOwner = cancelIfNotOwner
    }

    fun checkOnly(): Boolean {
        return checkOnly
    }

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    override fun isCancelled(): Boolean {
        var cancelled = super.isCancelled()
        if (cancelIfNotOwner) {
            cancelled = cancelled or (player.uniqueId != portal.owner())
        }
        return cancelled
    }

    companion object {
        @JvmStatic val handlerList: HandlerList = HandlerList()
    }
}
