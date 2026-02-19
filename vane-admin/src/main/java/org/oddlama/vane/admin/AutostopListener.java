package org.oddlama.vane.admin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.oddlama.vane.core.Listener;

public class AutostopListener extends Listener<Admin> {

    AutostopGroup autostop;

    public AutostopListener(AutostopGroup context) {
        super(context);
        this.autostop = context;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        autostop.abort();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        playerLeave(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerLeave(event.getPlayer());
    }

    private void playerLeave(final Player player) {
        var players = getModule().getServer().getOnlinePlayers();
        if (players.isEmpty() || (players.size() == 1 && players.iterator().next() == player)) {
            autostop.schedule();
        }
    }
}
