package org.oddlama.vane.portals.event

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.oddlama.vane.portals.portal.Portal

class PortalSelectTargetEvent(
    @JvmField val player: Player?,
    @JvmField val portal: Portal?,
    @JvmField val target: Portal?,
    private val checkOnly: Boolean
) : PortalEvent() {
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
