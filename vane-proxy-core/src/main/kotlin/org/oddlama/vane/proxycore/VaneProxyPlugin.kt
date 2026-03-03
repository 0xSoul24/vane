package org.oddlama.vane.proxycore

import org.oddlama.vane.proxycore.config.ConfigManager
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo
import org.oddlama.vane.proxycore.config.ManagedServer
import org.oddlama.vane.proxycore.config.ManagedServer.ConfigItemSource
import org.oddlama.vane.proxycore.listeners.PreLoginEvent.MultiplexedPlayer
import org.oddlama.vane.proxycore.log.IVaneLogger
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

abstract class VaneProxyPlugin {
    @JvmField
    var config: ConfigManager = ConfigManager(this)
    @JvmField
    var maintenance: Maintenance = Maintenance(this)
    @JvmField
    var logger: IVaneLogger? = null
    @JvmField
    var server: ProxyServer? = null
    @JvmField
    var dataFolder: File? = null

    val multiplexedUuids: LinkedHashMap<UUID?, UUID?> = LinkedHashMap<UUID?, UUID?>()
    @JvmField
    val pendingMultiplexerLogins: LinkedHashMap<UUID?, MultiplexedPlayer?> = LinkedHashMap<UUID?, MultiplexedPlayer?>()
    private var serverStarting = false

    fun isOnline(server: IVaneProxyServerInfo): Boolean {
        val addr = server.socketAddress
        if (addr !is InetSocketAddress) {
            return false
        }

        var connected = false
        try {
            Socket(addr.hostName, addr.port).use { test ->
                connected = test.isConnected
            }
        } catch (e: IOException) {
            // Server not up or not reachable
        }

        return connected
    }

    fun getMotd(server: IVaneProxyServerInfo): String {
        // Maintenance
        if (maintenance.enabled()) {
            return maintenance.formatMessage(Maintenance.MOTD)
        }

        val cms = config.managedServers[server.name] ?: return ""
        val source = if (isOnline(server)) {
            ConfigItemSource.ONLINE
        } else {
            ConfigItemSource.OFFLINE
        }

        return cms.motd(source)
    }

    fun getFavicon(server: IVaneProxyServerInfo): String? {
        val cms = config.managedServers[server.name] ?: return null
        val source = if (isOnline(server)) {
            ConfigItemSource.ONLINE
        } else {
            ConfigItemSource.OFFLINE
        }

        return cms.favicon(source)
    }

    val proxy: ProxyServer
        get() = server!!

    fun getLogger(): IVaneLogger {
        return logger!!
    }

    fun tryStartServer(server: ManagedServer) {
        // FIXME: this is not async-safe and there might be conditions where two start commands can
        // be executed
        // simultaneously. Don't rely on this as a user - instead use a start command that is
        // atomic.
        if (serverStarting) return

        // Ensure we have a scheduler to run the start task
        val scheduler = this.server?.scheduler
        if (scheduler == null) {
            getLogger().log(Level.SEVERE, "No scheduler available to start server '${server.id()}'")
            return
        }

        // Resolve and validate the start command before using the spread operator
        val rawCmd: Array<String?>? = server.startCmd()
        val cmd: Array<String>
        if (rawCmd == null) {
            getLogger().log(Level.SEVERE, "Start command for server '${server.id()}' is not configured.")
            return
        } else {
            // Filter out any null elements; if after filtering there are no args, abort
            val filtered = rawCmd.filterNotNull()
            if (filtered.isEmpty()) {
                getLogger().log(Level.SEVERE, "Start command for server '${server.id()}' is empty after filtering nulls.")
                return
            }
            cmd = filtered.toTypedArray()
        }

        scheduler.runAsync(this) {
            try {
                serverStarting = true
                getLogger()
                    .log(
                        Level.INFO,
                        "Running start command for server '${server.id()}': ${cmd.contentToString()}"
                    )
                val timeout = server.commandTimeout()

                val processBuilder = ProcessBuilder(*cmd)
                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
                val process = processBuilder.start()

                if (!process.waitFor(timeout!!.toLong(), TimeUnit.SECONDS)) {
                    getLogger().log(Level.SEVERE, "Server '${server.id()}'s start command timed out!")
                }

                if (process.exitValue() != 0) {
                    getLogger()
                        .log(
                            Level.SEVERE,
                            "Server '${server.id()}'s start command returned a nonzero exit code!"
                        )
                }
            } catch (e: Exception) {
                getLogger().log(Level.SEVERE, "Failed to start server '${server.id()}'", e)
            }
            serverStarting = false
        }
    }

    fun canJoinMaintenance(uuid: UUID?): Boolean {
        if (maintenance.enabled()) {
            // Client is connecting while maintenance is on
            // Players with a bypass_maintenance flag may join
            return this.server!!.hasPermission(uuid, "vane_proxy.bypass_maintenance")
        }

        return true
    }

    companion object {
        const val CHANNEL_AUTH_MULTIPLEX_NAMESPACE: String = "vane_proxy"
        const val CHANNEL_AUTH_MULTIPLEX_NAME: String = "auth_multiplex"
        const val CHANNEL_AUTH_MULTIPLEX: String = "$CHANNEL_AUTH_MULTIPLEX_NAMESPACE:$CHANNEL_AUTH_MULTIPLEX_NAME"
    }
}
