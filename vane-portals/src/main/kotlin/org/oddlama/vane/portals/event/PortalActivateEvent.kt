package org.oddlama.vane.portals.event

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.oddlama.vane.portals.portal.Portal

class PortalActivateEvent(@JvmField val player: Player?, @JvmField val portal: Portal?, val target: Portal?) : PortalEvent() {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic val handlerList: HandlerList = HandlerList()
    }
}
