package org.oddlama.vane.portals.event

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.oddlama.vane.portals.portal.Portal

class PortalOpenConsoleEvent(@JvmField val player: Player?, @JvmField val console: Block?, @JvmField val portal: Portal?) : PortalEvent() {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic val handlerList: HandlerList = HandlerList()
    }
}
