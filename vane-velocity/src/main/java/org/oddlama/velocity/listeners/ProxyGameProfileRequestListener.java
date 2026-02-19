package org.oddlama.velocity.listeners;

import static org.oddlama.velocity.Util.getServerForHost;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.GameProfile;
import java.util.logging.Level;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.VelocityCompatServerInfo;

public class ProxyGameProfileRequestListener {

    final Velocity velocity;

    public ProxyGameProfileRequestListener(Velocity velocity) {
        this.velocity = velocity;
    }

    @Subscribe(priority = 0)
    public void gameProfileRequest(final GameProfileRequestEvent event) {
        // ======= Check we even have a valid pending login =======

        final var virtualHost = event.getConnection().getVirtualHost();
        if (virtualHost.isEmpty()) return;

        final var multiplexer = velocity.getConfig().getMultiplexerForPort(virtualHost.get().getPort());
        if (multiplexer == null) return;

        final var pendingMultiplexerLogins = velocity.getPendingMultiplexerLogins();
        if (pendingMultiplexerLogins.isEmpty()) return;

        // ====================== End check ======================

        final var profile = event.getGameProfile();
        final var targetUuid = profile.getId();

        PreLoginEvent.MultiplexedPlayer player = pendingMultiplexerLogins.remove(targetUuid);
        if (player == null) {
            // We somehow have a multiplexer connection, but it wasn't registered in
            // `pendingMultiplexerLogins`
            // Not much to do here; the event isn't cancellable
            velocity.getLogger().log(Level.WARNING, "Unregistered multiplexer connection managed to get through!");
            return;
        }

        final GameProfile tamperedProfile = new GameProfile(player.newUuid, player.newName, profile.getProperties());
        event.setGameProfile(tamperedProfile);

        final var server = getServerForHost(velocity.getRawProxy(), virtualHost.get());
        final var serverInfo = new VelocityCompatServerInfo(server);
        PreLoginEvent.registerAuthMultiplexPlayer(serverInfo, player);

        // Now we can finally put our player in `multiplexed_uuids` :)
        velocity.getMultiplexedUuids().put(player.newUuid, player.originalUuid);
    }
}
