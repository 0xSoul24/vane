package org.oddlama.vane.portals.event

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.oddlama.vane.portals.portal.PortalBoundary

class PortalConstructEvent(@JvmField val player: Player?, @JvmField val boundary: PortalBoundary?, private val checkOnly: Boolean) :
    PortalEvent() {
    fun checkOnly(): Boolean {
        return checkOnly
    }

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic val handlerList: HandlerList = HandlerList()
    }
}
