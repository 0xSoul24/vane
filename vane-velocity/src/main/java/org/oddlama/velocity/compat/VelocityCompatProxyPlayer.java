package org.oddlama.velocity.compat;

import com.velocitypowered.api.proxy.Player;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.oddlama.vane.proxycore.ProxyPlayer;

public class VelocityCompatProxyPlayer implements ProxyPlayer {

    final Player player;

    public VelocityCompatProxyPlayer(Player player) {
        this.player = player;
    }

    @Override
    public void disconnect(String message) {
        player.disconnect(Component.text(message));
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public long getPing() {
        return player.getPing();
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(Component.text(message));
    }
}
