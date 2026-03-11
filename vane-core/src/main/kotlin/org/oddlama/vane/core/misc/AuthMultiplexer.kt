package org.oddlama.vane.core.misc

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.messaging.PluginMessageListener
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
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
    var storageAuthMultiplex: MutableMap<UUID?, UUID?> = mutableMapOf()

    @Persistent
    var storageAuthMultiplexerId: MutableMap<UUID?, Int?> = mutableMapOf()

    override fun onEnable() {
        super.onEnable()
        try {
            module!!.server.messenger.registerIncomingPluginChannel(module!!, CHANNEL_AUTH_MULTIPLEX, this)
        } catch (_: IllegalArgumentException) {
            // Channel already registered for this plugin; ignore to allow safe reloads
            module!!.log.info("AuthMultiplexer: incoming channel '$CHANNEL_AUTH_MULTIPLEX' already registered, skipping")
        }
    }

    override fun onDisable() {
        super.onDisable()
        try {
            module!!.server.messenger.unregisterIncomingPluginChannel(module!!, CHANNEL_AUTH_MULTIPLEX, this)
        } catch (e: Exception) {
            // Ignore issues during unregister to avoid noisy stack traces on shutdown
            module!!.log.fine("AuthMultiplexer: could not unregister incoming channel '$CHANNEL_AUTH_MULTIPLEX': ${e.message}")
        }
    }

    @Synchronized
    fun authMultiplexPlayerName(uuid: UUID?): String? {
        val originalPlayerId = storageAuthMultiplex[uuid] ?: return null
        val multiplexerId = storageAuthMultiplexerId[uuid] ?: return null
        val originalPlayer = module!!.server.getOfflinePlayer(originalPlayerId)
        return "§7[$multiplexerId]§r ${originalPlayer.name}"
    }

    private fun tryInitMultiplexedPlayerName(player: Player) {
        val id = player.uniqueId
        val displayName = authMultiplexPlayerName(id) ?: return

        module!!.log.info("[multiplex] Init player '$displayName' for registered auth multiplexed player {$id, ${player.name}}")

        val displayNameComponent = LegacyComponentSerializer.legacySection().deserialize(displayName)
        player.displayName(displayNameComponent)
        player.playerListName(displayNameComponent)

        val originalPlayerId = storageAuthMultiplex[id]
        val skin = try {
            resolveSkin(originalPlayerId)
        } catch (e: IOException) {
            module!!.log.log(Level.WARNING, "Failed to resolve skin for uuid '$id'", e)
            return
        } catch (e: URISyntaxException) {
            module!!.log.log(Level.WARNING, "Failed to resolve skin for uuid '$id'", e)
            return
        }

        player.playerProfile = player.playerProfile.also {
            it.setProperty(ProfileProperty("textures", skin.texture!!, skin.signature))
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        tryInitMultiplexedPlayerName(event.player)
    }

    @Synchronized
    override fun onPluginMessageReceived(channel: String, player: Player, bytes: ByteArray) {
        if (channel != CHANNEL_AUTH_MULTIPLEX) return

        try {
            DataInputStream(ByteArrayInputStream(bytes)).use { input ->
                val multiplexerId = input.readInt()
                val oldUuid = UUID.fromString(input.readUTF())
                val oldName = input.readUTF()
                val newUuid = UUID.fromString(input.readUTF())
                val newName = input.readUTF()

                module!!.log.info(
                    "[multiplex] Registered auth multiplexed player {$newUuid, $newName} " +
                    "from player {$oldUuid, $oldName} multiplexerId $multiplexerId"
                )
                storageAuthMultiplex[newUuid] = oldUuid
                storageAuthMultiplexerId[newUuid] = multiplexerId
                markPersistentStorageDirty()

                val multiplexedPlayer = module!!.server.getOfflinePlayer(newUuid)
                if (multiplexedPlayer.isOnline) {
                    tryInitMultiplexedPlayerName(multiplexedPlayer.player!!)
                }
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
