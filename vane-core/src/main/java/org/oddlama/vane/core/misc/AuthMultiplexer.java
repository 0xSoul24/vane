package org.oddlama.vane.core.misc;

import static org.oddlama.vane.util.Resolve.resolveSkin;

import com.destroystokyo.paper.profile.ProfileProperty;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.Resolve;

public class AuthMultiplexer extends Listener<Core> implements PluginMessageListener {

    // Channel for proxy messages to multiplex connections
    public static final String CHANNEL_AUTH_MULTIPLEX = "vane_proxy:auth_multiplex";

    // Persistent storage
    @Persistent
    public Map<UUID, UUID> storageAuthMultiplex = new HashMap<>();

    @Persistent
    public Map<UUID, Integer> storageAuthMultiplexerId = new HashMap<>();

    public AuthMultiplexer(Context<Core> context) {
        super(context);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        getModule()
            .getServer()
            .getMessenger()
            .registerIncomingPluginChannel(getModule(), CHANNEL_AUTH_MULTIPLEX, this);
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        getModule()
            .getServer()
            .getMessenger()
            .unregisterIncomingPluginChannel(getModule(), CHANNEL_AUTH_MULTIPLEX, this);
    }

    public synchronized String authMultiplexPlayerName(final UUID uuid) {
        final var originalPlayerId = storageAuthMultiplex.get(uuid);
        final var multiplexerId = storageAuthMultiplexerId.get(uuid);
        if (originalPlayerId == null || multiplexerId == null) {
            return null;
        }

        final var originalPlayer = getModule().getServer().getOfflinePlayer(originalPlayerId);
        return "§7[" + multiplexerId + "]§r " + originalPlayer.getName();
    }

    private void tryInitMultiplexedPlayerName(final Player player) {
        final var id = player.getUniqueId();
        final var displayName = authMultiplexPlayerName(id);
        if (displayName == null) {
            return;
        }

        getModule()
            .log.info(
                "[multiplex] Init player '" +
                displayName +
                "' for registered auth multiplexed player {" +
                id +
                ", " +
                player.getName() +
                "}"
            );
        final var displayNameComponent = LegacyComponentSerializer.legacySection().deserialize(displayName);
        player.displayName(displayNameComponent);
        player.playerListName(displayNameComponent);

        final var originalPlayerId = storageAuthMultiplex.get(id);
        Resolve.Skin skin;
        try {
            skin = resolveSkin(originalPlayerId);
        } catch (IOException | URISyntaxException e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to resolve skin for uuid '" + id + "'", e);
            return;
        }

        final var profile = player.getPlayerProfile();
        profile.setProperty(new ProfileProperty("textures", skin.texture, skin.signature));
        player.setPlayerProfile(profile);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerJoin(PlayerJoinEvent event) {
        tryInitMultiplexedPlayerName(event.getPlayer());
    }

    @Override
    public synchronized void onPluginMessageReceived(final String channel, final Player player, byte[] bytes) {
        if (!channel.equals(CHANNEL_AUTH_MULTIPLEX)) {
            return;
        }

        final var stream = new ByteArrayInputStream(bytes);
        final var in = new DataInputStream(stream);

        try {
            final var multiplexerId = in.readInt();
            final var oldUuid = UUID.fromString(in.readUTF());
            final var oldName = in.readUTF();
            final var newUuid = UUID.fromString(in.readUTF());
            final var newName = in.readUTF();

            getModule()
                .log.info(
                    "[multiplex] Registered auth multiplexed player {" +
                    newUuid +
                    ", " +
                    newName +
                    "} from player {" +
                    oldUuid +
                    ", " +
                    oldName +
                    "} multiplexerId " +
                    multiplexerId
                );
            storageAuthMultiplex.put(newUuid, oldUuid);
            storageAuthMultiplexerId.put(newUuid, multiplexerId);
            markPersistentStorageDirty();

            final var multiplexedPlayer = getModule().getServer().getOfflinePlayer(newUuid);
            if (multiplexedPlayer.isOnline()) {
                tryInitMultiplexedPlayerName(multiplexedPlayer.getPlayer());
            }
        } catch (IOException e) {
            getModule().log.log(Level.SEVERE, "Failed to process auth multiplex message", e);
        }
    }
}
