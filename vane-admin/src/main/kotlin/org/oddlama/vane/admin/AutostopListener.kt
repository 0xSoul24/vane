package org.oddlama.vane.admin

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.oddlama.vane.core.Listener

class AutostopListener(var autostop: AutostopGroup) : Listener<Admin?>(autostop) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent?) {
        autostop.abort()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerKick(event: PlayerKickEvent) {
        playerLeave(event.getPlayer())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        playerLeave(event.getPlayer())
    }

    private fun playerLeave(player: Player?) {
        val players: MutableCollection<out Player?> = module!!.server.onlinePlayers
        if (players.isEmpty() || (players.size == 1 && players.iterator().next() === player)) {
            autostop.schedule()
        }
    }
}
