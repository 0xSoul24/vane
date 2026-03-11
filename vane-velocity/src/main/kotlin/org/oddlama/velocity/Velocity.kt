package org.oddlama.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.proxy.VelocityServer
import com.velocitypowered.proxy.network.ConnectionManager
import org.bstats.velocity.Metrics
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.log.Slf4jCompatLogger
import org.oddlama.vane.proxycore.util.VERSION
import org.oddlama.velocity.commands.Maintenance
import org.oddlama.velocity.commands.Ping
import org.oddlama.velocity.compat.VelocityCompatProxyServer
import org.oddlama.velocity.listeners.*
import org.slf4j.Logger
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.function.BiConsumer
import java.util.logging.Level

@Plugin(
    id = "vane-velocity",
    name = "Vane Velocity",
    version = VERSION,
    description = "TODO",
    authors = ["oddlama", "Serial-ATA"],
    url = "https://github.com/oddlama/vane"
)
class Velocity @Inject constructor(
    server: ProxyServer,
    logger: Logger,
    metricsFactory: Metrics.Factory,
    @DataDirectory dataDir: Path
) : VaneProxyPlugin() {
    val rawProxy: ProxyServer

    // bStats
    @Suppress("unused")
    private val metricsFactory: Metrics.Factory

    init {
        this.server = VelocityCompatProxyServer(server)
        this.logger = Slf4jCompatLogger(logger)

        this.metricsFactory = metricsFactory

        this.rawProxy = server
        this.dataFolder = dataDir.toFile()
    }

    @Subscribe
    fun onEnable(event: ProxyInitializeEvent?) {
        if (!config.load()) {
            disable()
            return
        }

        metricsFactory.make(this, 8891)

        val eventManager = rawProxy.eventManager

        eventManager.register(this, ProxyPingListener(this))
        eventManager.register(this, ProxyPreLoginListener(this))
        eventManager.register(this, ProxyGameProfileRequestListener(this))
        eventManager.register(this, ProxyLoginListener(this))
        eventManager.register(this, ProxyDisconnectListener(this))

        maintenance.load()

        val commandManager = rawProxy.commandManager

        val pingMeta = commandManager.metaBuilder("ping").build()
        commandManager.register(pingMeta, Ping(this))

        val maintenanceMeta = commandManager.metaBuilder("maintenance").build()
        commandManager.register(maintenanceMeta, Maintenance(this))

        rawProxy.channelRegistrar.register(CHANNEL)

        if (!config.multiplexerById.isEmpty()) {
            try {
                getLogger().log(Level.INFO, "Attempting to register auth multiplexers")

                // Velocity doesn't let you register multiple listeners like Bungeecord,
                // So we have to take matters into our own hands :)
                handleListeners(
                    "Registering",
                    BiConsumer { obj: ConnectionManager?, address: InetSocketAddress? -> obj!!.bind(address) })
            } catch (e: Exception) {
                getLogger().log(Level.SEVERE, "Failed to inject into VelocityServer!", e)
                disable()
            }
        }
    }

    @Subscribe
    fun onDisable(event: ProxyShutdownEvent?) {
        disable()
    }

    private fun disable() {
        rawProxy.eventManager.unregisterListeners(this)

        rawProxy.channelRegistrar.unregister(CHANNEL)

        // Now let's be good and clean up our mess :)
        try {
            getLogger().log(Level.INFO, "Attempting to close auth multiplexers")

            handleListeners(
                "Closing",
                BiConsumer { obj: ConnectionManager?, oldBind: InetSocketAddress? -> obj!!.close(oldBind) })
        } catch (e: Exception) {
            getLogger().log(Level.SEVERE, "Failed to stop listeners!", e)
            getLogger().log(Level.SEVERE, "Shutting down the server to prevent lingering unmanaged listeners!")
            rawProxy.shutdown()
        }

        server = null
        logger = null
    }

    @Throws(ClassNotFoundException::class, NoSuchFieldException::class, IllegalAccessException::class)
    private fun handleListeners(
        action: String?,
        method: BiConsumer<in ConnectionManager?, in InetSocketAddress?>
    ) {
        val server = this.rawProxy as VelocityServer?

        // We steal the VelocityServer's `ConnectionManager`, which (currently) has no
        // issue binding to however many addresses we give it.
        val velocityServer = Class.forName("com.velocitypowered.proxy.VelocityServer")
        val cmField = velocityServer.getDeclaredField("cm")
        cmField.setAccessible(true)

        val cm = cmField.get(server) as ConnectionManager?

        for (multiplexerMap in config.multiplexerById.entries) {
            val id = multiplexerMap.key
            val port = multiplexerMap.value!!.port

            getLogger().log(Level.INFO, "$action multiplexer ID $id, bound to port $port")

            val address = InetSocketAddress(port!!)
            method.accept(cm, address)
        }
    }

    companion object {
        @JvmField
        val CHANNEL: MinecraftChannelIdentifier = MinecraftChannelIdentifier.create(
            CHANNEL_AUTH_MULTIPLEX_NAMESPACE,
            CHANNEL_AUTH_MULTIPLEX_NAME
        )
    }
}
