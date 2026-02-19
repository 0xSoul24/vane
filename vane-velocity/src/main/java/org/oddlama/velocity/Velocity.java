package org.oddlama.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.ConnectionManager;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import org.bstats.velocity.Metrics;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.log.slf4jCompatLogger;
import org.oddlama.vane.proxycore.util.Version;
import org.oddlama.velocity.commands.Maintenance;
import org.oddlama.velocity.commands.Ping;
import org.oddlama.velocity.compat.VelocityCompatProxyServer;
import org.oddlama.velocity.listeners.*;
import org.slf4j.Logger;

@Plugin(
    id = "vane-velocity",
    name = "Vane Velocity",
    version = Version.VERSION,
    description = "TODO",
    authors = { "oddlama", "Serial-ATA" },
    url = "https://github.com/oddlama/vane"
)
public class Velocity extends VaneProxyPlugin {

    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create(
        CHANNEL_AUTH_MULTIPLEX_NAMESPACE,
        CHANNEL_AUTH_MULTIPLEX_NAME
    );
    private final ProxyServer velocityServer;

    // bStats
    @SuppressWarnings("unused")
    private final Metrics.Factory metricsFactory;

    @Inject
    public Velocity(
        ProxyServer server,
        Logger logger,
        Metrics.Factory metricsFactory,
        @DataDirectory final Path dataDir
    ) {
        this.server = new VelocityCompatProxyServer(server);
        this.logger = new slf4jCompatLogger(logger);

        this.metricsFactory = metricsFactory;

        this.velocityServer = server;
        this.dataDir = dataDir.toFile();
    }

    public ProxyServer getRawProxy() {
        return velocityServer;
    }

    @Subscribe
    public void onEnable(final ProxyInitializeEvent event) {
        if (!config.load()) {
            disable();
            return;
        }

        metricsFactory.make(this, 8891);

        EventManager eventManager = velocityServer.getEventManager();

        eventManager.register(this, new ProxyPingListener(this));
        eventManager.register(this, new ProxyPreLoginListener(this));
        eventManager.register(this, new ProxyGameProfileRequestListener(this));
        eventManager.register(this, new ProxyLoginListener(this));
        eventManager.register(this, new ProxyDisconnectListener(this));

        maintenance.load();

        CommandManager commandManager = velocityServer.getCommandManager();

        CommandMeta pingMeta = commandManager.metaBuilder("ping").build();
        commandManager.register(pingMeta, new Ping(this));

        CommandMeta maintenanceMeta = commandManager.metaBuilder("maintenance").build();
        commandManager.register(maintenanceMeta, new Maintenance(this));

        velocityServer.getChannelRegistrar().register(CHANNEL);

        if (!config.multiplexerById.isEmpty()) {
            try {
                getLogger().log(Level.INFO, "Attempting to register auth multiplexers");

                // Velocity doesn't let you register multiple listeners like Bungeecord,
                // So we have to take matters into our own hands :)
                handleListeners("Registering", ConnectionManager::bind);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to inject into VelocityServer!", e);
                disable();
            }
        }
    }

    @Subscribe
    public void onDisable(final ProxyShutdownEvent event) {
        disable();
    }

    private void disable() {
        velocityServer.getEventManager().unregisterListeners(this);

        velocityServer.getChannelRegistrar().unregister(CHANNEL);

        // Now let's be good and clean up our mess :)
        try {
            getLogger().log(Level.INFO, "Attempting to close auth multiplexers");

            handleListeners("Closing", ConnectionManager::close);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to stop listeners!", e);
            getLogger().log(Level.SEVERE, "Shutting down the server to prevent lingering unmanaged listeners!");
            velocityServer.shutdown();
        }

        server = null;
        logger = null;
    }

    private void handleListeners(
        String action,
        BiConsumer<? super ConnectionManager, ? super InetSocketAddress> method
    ) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        final var server = (VelocityServer) velocityServer;

        // We steal the VelocityServer's `ConnectionManager`, which (currently) has no
        // issue binding to however many addresses we give it.
        final var velocityServer = Class.forName("com.velocitypowered.proxy.VelocityServer");
        final var cmField = velocityServer.getDeclaredField("cm");
        cmField.setAccessible(true);

        final var cm = (ConnectionManager) cmField.get(server);

        for (final var multiplexerMap : config.multiplexerById.entrySet()) {
            final var id = multiplexerMap.getKey();
            final var port = multiplexerMap.getValue().port;

            getLogger().log(Level.INFO, action + " multiplexer ID " + id + ", bound to port " + port);

            final var address = new InetSocketAddress(port);
            method.accept(cm, address);
        }
    }
}
