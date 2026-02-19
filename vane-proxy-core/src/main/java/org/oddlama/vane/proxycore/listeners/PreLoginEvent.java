package org.oddlama.vane.proxycore.listeners;

import static org.oddlama.vane.proxycore.Util.addUuid;
import static org.oddlama.vane.proxycore.util.Resolve.resolveUuid;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.logging.Level;
import org.oddlama.vane.proxycore.Maintenance;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;

public abstract class PreLoginEvent implements ProxyEvent, ProxyCancellableEvent {

    public static String MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK =
        "§cYou have no permission to use this auth multiplexer!";

    public VaneProxyPlugin plugin;

    public PreLoginEvent(VaneProxyPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire() {
        // Not applicable
        assert false;
    }

    public void fire(PreLoginDestination destination) {
        ProxyPendingConnection connection = getConnection();

        // Multiplex authentication if the connection is to a multiplexing port
        final var port = connection.getPort();
        final var multiplexer = plugin.getConfig().getMultiplexerForPort(port);
        if (multiplexer == null) return;

        final var multiplexerId = multiplexer.getKey();

        // This is pre-authentication, so we need to resolve the uuid ourselves.
        String playerName = connection.getName();
        UUID uuid;

        try {
            uuid = resolveUuid(playerName);
        } catch (IOException | URISyntaxException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to resolve UUID for player '" + playerName + "'", e);
            return;
        }

        if (!plugin.canJoinMaintenance(uuid)) {
            this.cancel(plugin.getMaintenance().formatMessage(Maintenance.MESSAGE_CONNECT));
            return;
        }

        if (!multiplexer.getValue().uuidIsAllowed(uuid)) {
            this.cancel(MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK);
            return;
        }

        final var name = connection.getName();
        final var newUuid = addUuid(uuid, multiplexerId);
        final var newUuidStr = newUuid.toString();
        final var newName = newUuidStr.substring(newUuidStr.length() - 16).replace("-", "_");

        plugin
            .getLogger()
            .log(
                Level.INFO,
                "auth multiplex request from player " +
                name +
                " connecting from " +
                connection.getSocketAddress().toString()
            );

        MultiplexedPlayer multiplexedPlayer = new MultiplexedPlayer(multiplexerId, name, newName, uuid, newUuid);
        if (!implementationSpecificAuth(multiplexedPlayer)) {
            return;
        }

        switch (destination) {
            case MULTIPLEXED_UUIDS -> plugin
                .getMultiplexedUuids()
                .put(multiplexedPlayer.newUuid, multiplexedPlayer.originalUuid);
            case PENDING_MULTIPLEXED_LOGINS -> plugin.getPendingMultiplexerLogins().put(uuid, multiplexedPlayer);
        }
    }

    public abstract boolean implementationSpecificAuth(MultiplexedPlayer multiplexedPlayer);

    public static void registerAuthMultiplexPlayer(
        IVaneProxyServerInfo server,
        PreLoginEvent.MultiplexedPlayer multiplexedPlayer
    ) {
        final var stream = new ByteArrayOutputStream();
        final var out = new DataOutputStream(stream);

        try {
            out.writeInt(multiplexedPlayer.multiplexerId);
            out.writeUTF(multiplexedPlayer.originalUuid.toString());
            out.writeUTF(multiplexedPlayer.name);
            out.writeUTF(multiplexedPlayer.newUuid.toString());
            out.writeUTF(multiplexedPlayer.newName);
        } catch (IOException e) {
            // This should not happen in a ByteArrayOutputStream, but log it for diagnostics
            java.util.logging.Logger.getLogger(PreLoginEvent.class.getName())
                .log(java.util.logging.Level.SEVERE, "Failed to write multiplexed player data", e);
        }

        server.sendData(stream.toByteArray());
    }

    /** Where to send the details of a PreLoginEvent */
    public enum PreLoginDestination {
        MULTIPLEXED_UUIDS,
        PENDING_MULTIPLEXED_LOGINS,
    }

    public static class MultiplexedPlayer {

        public Integer multiplexerId;
        public String name;
        public String newName;
        public UUID originalUuid;
        public UUID newUuid;

        public MultiplexedPlayer(
            Integer multiplexerId,
            String name,
            String newName,
            UUID originalUuid,
            UUID newUuid
        ) {
            this.multiplexerId = multiplexerId;
            this.name = name;
            this.newName = newName;
            this.originalUuid = originalUuid;
            this.newUuid = newUuid;
        }
    }
}
