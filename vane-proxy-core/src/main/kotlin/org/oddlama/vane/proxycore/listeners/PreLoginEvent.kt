package org.oddlama.vane.proxycore.listeners

import org.oddlama.vane.proxycore.Maintenance
import org.oddlama.vane.proxycore.Util
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.config.AuthMultiplex
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo
import org.oddlama.vane.util.Resolve.resolveUuid
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.URISyntaxException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

abstract class PreLoginEvent(@JvmField var plugin: VaneProxyPlugin) : ProxyEvent, ProxyCancellableEvent {
    override fun fire() {
        // Not applicable
        assert(false)
    }

    fun fire(destination: PreLoginDestination) {
        val connection = connection

        // Multiplex authentication if the connection is to a multiplexing port
        val port = connection!!.port
        val multiplexer: MutableMap.MutableEntry<Int?, AuthMultiplex?> =
            plugin.config.getMultiplexerForPort(port) ?: return

        val multiplexerId: Int? = multiplexer.key

        // This is pre-authentication, so we need to resolve the uuid ourselves.
        val playerName = connection.name ?: return
        val uuid: UUID

        try {
            uuid = resolveUuid(playerName)
        } catch (e: IOException) {
            plugin.getLogger().log(Level.WARNING, "Failed to resolve UUID for player '$playerName'", e)
            return
        } catch (e: URISyntaxException) {
            plugin.getLogger().log(Level.WARNING, "Failed to resolve UUID for player '$playerName'", e)
            return
        }

        if (!plugin.canJoinMaintenance(uuid)) {
            this.cancel(plugin.maintenance.formatMessage(Maintenance.MESSAGE_CONNECT))
            return
        }

        if (!multiplexer.value!!.uuidIsAllowed(uuid)) {
            this.cancel(MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK)
            return
        }

        val name = connection.name
        val newUuid: UUID = Util.addUuid(uuid, multiplexerId!!.toLong())
        val newUuidStr: String = newUuid.toString()
        val newName: String = newUuidStr.substring(newUuidStr.length - 16).replace("-", "_")

        plugin
            .getLogger()
            .log(
                Level.INFO,
                "auth multiplex request from player " +
                        name +
                        " connecting from " +
                        connection.socketAddress.toString()
            )

        val multiplexedPlayer = MultiplexedPlayer(multiplexerId, name!!, newName, uuid, newUuid)
        if (!implementationSpecificAuth(multiplexedPlayer)) {
            return
        }

        when (destination) {
            PreLoginDestination.MULTIPLEXED_UUIDS -> plugin
                .multiplexedUuids[multiplexedPlayer.newUuid] = multiplexedPlayer.originalUuid

            PreLoginDestination.PENDING_MULTIPLEXED_LOGINS -> plugin.pendingMultiplexerLogins[uuid] = multiplexedPlayer
        }
    }

    abstract fun implementationSpecificAuth(multiplexedPlayer: MultiplexedPlayer?): Boolean

    /** Where to send the details of a PreLoginEvent  */
    enum class PreLoginDestination {
        MULTIPLEXED_UUIDS,
        PENDING_MULTIPLEXED_LOGINS,
    }

    class MultiplexedPlayer(
        @JvmField var multiplexerId: Int,
        @JvmField var name: String,
        @JvmField var newName: String,
        @JvmField var originalUuid: UUID,
        @JvmField var newUuid: UUID
    )

    companion object {
        var MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK: String =
            "§cYou have no permission to use this auth multiplexer!"

        @JvmStatic
        fun registerAuthMultiplexPlayer(
            server: IVaneProxyServerInfo,
            multiplexedPlayer: MultiplexedPlayer
        ) {
            val stream = ByteArrayOutputStream()
            val out = DataOutputStream(stream)

            try {
                out.writeInt(multiplexedPlayer.multiplexerId)
                out.writeUTF(multiplexedPlayer.originalUuid.toString())
                out.writeUTF(multiplexedPlayer.name)
                out.writeUTF(multiplexedPlayer.newUuid.toString())
                out.writeUTF(multiplexedPlayer.newName)
            } catch (e: IOException) {
                // This should not happen in a ByteArrayOutputStream, but log it for diagnostics
                Logger.getLogger(PreLoginEvent::class.java.getName())
                    .log(Level.SEVERE, "Failed to write multiplexed player data", e)
            }

            server.sendData(stream.toByteArray())
        }
    }
}