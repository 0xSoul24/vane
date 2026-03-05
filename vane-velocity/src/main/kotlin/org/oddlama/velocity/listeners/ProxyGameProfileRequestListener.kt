package org.oddlama.velocity.listeners

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.GameProfileRequestEvent
import com.velocitypowered.api.util.GameProfile
import org.oddlama.vane.proxycore.listeners.PreLoginEvent.Companion.registerAuthMultiplexPlayer
import org.oddlama.velocity.Util.getServerForHost
import org.oddlama.velocity.Velocity
import org.oddlama.velocity.compat.VelocityCompatServerInfo
import java.util.logging.Level

class ProxyGameProfileRequestListener(val velocity: Velocity) {
    @Subscribe(priority = 0)
    fun gameProfileRequest(event: GameProfileRequestEvent) {
        // ======= Check we even have a valid pending login =======

        val virtualHost = event.connection.virtualHost
        if (virtualHost.isEmpty) return

        val multiplexer = velocity.config.getMultiplexerForPort(virtualHost.get().port) ?: return

        val pendingMultiplexerLogins = velocity.pendingMultiplexerLogins
        if (pendingMultiplexerLogins.isEmpty()) return

        // ====================== End check ======================
        val profile = event.gameProfile
        val targetUuid = profile.id

        val player = pendingMultiplexerLogins.remove(targetUuid)
        if (player == null) {
            // We somehow have a multiplexer connection, but it wasn't registered in
            // `pendingMultiplexerLogins`
            // Not much to do here; the event isn't cancellable
            velocity.getLogger().log(Level.WARNING, "Unregistered multiplexer connection managed to get through!")
            return
        }

        val tamperedProfile = GameProfile(player.newUuid, player.newName, profile.properties)
        event.setGameProfile(tamperedProfile)

        val server = getServerForHost(velocity.rawProxy, virtualHost.get())
        val serverInfo = VelocityCompatServerInfo(server)
        registerAuthMultiplexPlayer(serverInfo, player)

        // Now we can finally put our player in `multiplexed_uuids` :)
        velocity.multiplexedUuids[player.newUuid] = player.originalUuid
    }
}
