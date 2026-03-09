package org.oddlama.vane.trifles.event

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerTeleportEvent

class PlayerTeleportScrollEvent(player: Player, from: Location, to: Location?) :
    PlayerTeleportEvent(player, from, to, TeleportCause.PLUGIN) {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        val handlerList: HandlerList = HandlerList()
    }
}
