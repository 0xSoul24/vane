package org.oddlama.vane.portals.event

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.oddlama.vane.portals.portal.Portal

class PortalLinkConsoleEvent(
    @JvmField val player: Player,
    @JvmField val console: Block?,
    val portalBlocks: MutableList<Block?>?,
    private val checkOnly: Boolean,
    @JvmField val portal: Portal?
) : PortalEvent() {
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
        if (cancelIfNotOwner && portal != null) {
            cancelled = cancelled or (player.uniqueId != portal.owner())
        }
        return cancelled
    }

    companion object {
        @JvmStatic val handlerList: HandlerList = HandlerList()
    }
}
