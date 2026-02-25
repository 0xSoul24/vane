package org.oddlama.vane.core.misc

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.messaging.PluginMessageListener
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.Resolve
import org.oddlama.vane.util.Resolve.resolveSkin
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.net.URISyntaxException
import java.util.*
import java.util.logging.Level

class AuthMultiplexer(context: Context<Core?>?) : Listener<Core?>(context), PluginMessageListener {
    // Persistent storage
    @Persistent
    var storageAuthMultiplex: MutableMap<UUID?, UUID?> = HashMap<UUID?, UUID?>()

    @Persistent
    var storageAuthMultiplexerId: MutableMap<UUID?, Int?> = HashMap<UUID?, Int?>()

    override fun onEnable() {
        super.onEnable()
        try {
            module!!
                .server
                .messenger
                .registerIncomingPluginChannel(module!!, CHANNEL_AUTH_MULTIPLEX, this)
        } catch (e: IllegalArgumentException) {
            // Channel already registered for this plugin; ignore to allow safe reloads
            module!!.log.info("AuthMultiplexer: incoming channel '${CHANNEL_AUTH_MULTIPLEX}' already registered, skipping")
        }
    }

    override fun onDisable() {
        super.onDisable()
        try {
            module!!
                .server
                .messenger
                .unregisterIncomingPluginChannel(module!!, CHANNEL_AUTH_MULTIPLEX, this)
        } catch (e: Exception) {
            // Ignore issues during unregister to avoid noisy stack traces on shutdown
            module!!.log.fine("AuthMultiplexer: could not unregister incoming channel '${CHANNEL_AUTH_MULTIPLEX}': ${e.message}")
        }
    }

    @Synchronized
    fun authMultiplexPlayerName(uuid: UUID?): String? {
        val originalPlayerId = storageAuthMultiplex[uuid]
        val multiplexerId = storageAuthMultiplexerId[uuid]
        if (originalPlayerId == null || multiplexerId == null) {
            return null
        }

        val originalPlayer = module!!.server.getOfflinePlayer(originalPlayerId)
        return "§7[" + multiplexerId + "]§r " + originalPlayer.name
    }

    private fun tryInitMultiplexedPlayerName(player: Player) {
        val id = player.uniqueId
        val displayName = authMultiplexPlayerName(id) ?: return

        module!!
            .log.info(
                "[multiplex] Init player '" +
                        displayName +
                        "' for registered auth multiplexed player {" +
                        id +
                        ", " +
                        player.name +
                        "}"
            )
        val displayNameComponent = LegacyComponentSerializer.legacySection().deserialize(displayName)
        player.displayName(displayNameComponent)
        player.playerListName(displayNameComponent)

        val originalPlayerId = storageAuthMultiplex[id]
        val skin: Resolve.Skin?
        try {
            skin = resolveSkin(originalPlayerId)
        } catch (e: IOException) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to resolve skin for uuid '$id'", e)
            return
        } catch (e: URISyntaxException) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to resolve skin for uuid '$id'", e)
            return
        }

        val profile = player.playerProfile
        profile.setProperty(ProfileProperty("textures", skin.texture!!, skin.signature))
        player.playerProfile = profile
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        tryInitMultiplexedPlayerName(event.getPlayer())
    }

    @Synchronized
    override fun onPluginMessageReceived(channel: String, player: Player, bytes: ByteArray) {
        if (channel != CHANNEL_AUTH_MULTIPLEX) {
            return
        }

        val stream = ByteArrayInputStream(bytes)
        val `in` = DataInputStream(stream)

        try {
            val multiplexerId = `in`.readInt()
            val oldUuid = UUID.fromString(`in`.readUTF())
            val oldName = `in`.readUTF()
            val newUuid = UUID.fromString(`in`.readUTF())
            val newName = `in`.readUTF()

            module!!
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
                )
            storageAuthMultiplex[newUuid] = oldUuid
            storageAuthMultiplexerId[newUuid] = multiplexerId
            markPersistentStorageDirty()

            val multiplexedPlayer = module!!.server.getOfflinePlayer(newUuid)
            if (multiplexedPlayer.isOnline) {
                tryInitMultiplexedPlayerName(multiplexedPlayer.player!!)
            }
        } catch (e: IOException) {
            module!!.log.log(Level.SEVERE, "Failed to process auth multiplex message", e)
        }
    }

    companion object {
        // Channel for proxy messages to multiplex connections
        const val CHANNEL_AUTH_MULTIPLEX: String = "vane_proxy:auth_multiplex"
    }
}
