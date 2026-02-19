package org.oddlama.vane.proxycore;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.proxycore.config.ConfigManager;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.proxycore.config.ManagedServer;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.vane.proxycore.log.IVaneLogger;

public abstract class VaneProxyPlugin {

    public static final String CHANNEL_AUTH_MULTIPLEX_NAMESPACE = "vane_proxy";
    public static final String CHANNEL_AUTH_MULTIPLEX_NAME = "auth_multiplex";
    public static final String CHANNEL_AUTH_MULTIPLEX =
        CHANNEL_AUTH_MULTIPLEX_NAMESPACE + ":" + CHANNEL_AUTH_MULTIPLEX_NAME;

    public ConfigManager config = new ConfigManager(this);
    public Maintenance maintenance = new Maintenance(this);
    public IVaneLogger logger;
    public ProxyServer server;
    public File dataDir;

    private final LinkedHashMap<UUID, UUID> multiplexedUUIDs = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, PreLoginEvent.MultiplexedPlayer> pendingMultiplexerLogins =
        new LinkedHashMap<>();
    private boolean serverStarting;

    public boolean isOnline(final IVaneProxyServerInfo server) {
        final var addr = server.getSocketAddress();
        if (!(addr instanceof final InetSocketAddress inetAddr)) {
            return false;
        }

        var connected = false;
        try (final var test = new Socket(inetAddr.getHostName(), inetAddr.getPort())) {
            connected = test.isConnected();
        } catch (IOException e) {
            // Server not up or not reachable
        }

        return connected;
    }

    public String getMotd(final IVaneProxyServerInfo server) {
        // Maintenance
        if (maintenance.enabled()) {
            return maintenance.formatMessage(Maintenance.MOTD);
        }

        final var cms = config.managedServers.get(server.getName());
        if (cms == null) return "";

        ManagedServer.ConfigItemSource source;
        if (isOnline(server)) {
            source = ManagedServer.ConfigItemSource.ONLINE;
        } else {
            source = ManagedServer.ConfigItemSource.OFFLINE;
        }

        return cms.motd(source);
    }

    public @Nullable String getFavicon(final IVaneProxyServerInfo server) {
        final var cms = config.managedServers.get(server.getName());
        if (cms == null) return null;

        ManagedServer.ConfigItemSource source;
        if (isOnline(server)) {
            source = ManagedServer.ConfigItemSource.ONLINE;
        } else {
            source = ManagedServer.ConfigItemSource.OFFLINE;
        }

        return cms.favicon(source);
    }

    public File getDataFolder() {
        return dataDir;
    }

    public ProxyServer getProxy() {
        return server;
    }

    public @NotNull IVaneLogger getLogger() {
        return logger;
    }

    public @NotNull Maintenance getMaintenance() {
        return this.maintenance;
    }

    public @NotNull ConfigManager getConfig() {
        return this.config;
    }

    public void tryStartServer(ManagedServer server) {
        // FIXME: this is not async-safe and there might be conditions where two start commands can
        // be executed
        // simultaneously. Don't rely on this as a user - instead use a start command that is
        // atomic.
        if (serverStarting) return;

        this.server.getScheduler()
            .runAsync(this, () -> {
                try {
                    serverStarting = true;
                    getLogger()
                        .log(
                            Level.INFO,
                            "Running start command for server '" +
                            server.id() +
                            "': " +
                            Arrays.toString(server.startCmd())
                        );
                    final var timeout = server.commandTimeout();

                    final var processBuilder = new ProcessBuilder(server.startCmd());
                    processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
                    final var process = processBuilder.start();

                    if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                        getLogger().log(Level.SEVERE, "Server '" + server.id() + "'s start command timed out!");
                    }

                    if (process.exitValue() != 0) {
                        getLogger()
                            .log(
                                Level.SEVERE,
                                "Server '" + server.id() + "'s start command returned a nonzero exit code!"
                            );
                    }
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to start server '" + server.id() + "'", e);
                }

                serverStarting = false;
            });
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canJoinMaintenance(UUID uuid) {
        if (maintenance.enabled()) {
            // Client is connecting while maintenance is on
            // Players with a bypass_maintenance flag may join
            return this.server.hasPermission(uuid, "vane_proxy.bypass_maintenance");
        }

        return true;
    }

    public LinkedHashMap<UUID, UUID> getMultiplexedUuids() {
        return multiplexedUUIDs;
    }

    public LinkedHashMap<UUID, PreLoginEvent.MultiplexedPlayer> getPendingMultiplexerLogins() {
        return pendingMultiplexerLogins;
    }
}
