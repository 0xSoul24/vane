package org.oddlama.vane.proxycore.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.proxycore.VaneProxyPlugin;

public class ConfigManager {

    // name → managed server
    public final Map<String, ManagedServer> managedServers = new HashMap<>();
    // port → alias id (starts at 1)
    public final Map<Integer, AuthMultiplex> multiplexerById = new HashMap<>();
    private final VaneProxyPlugin plugin;

    public ConfigManager(final VaneProxyPlugin plugin) {
        this.plugin = plugin;
    }

    private File file() {
        return new File(plugin.getDataFolder(), "config.toml");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean load() {
        final var file = file();
        if (!file.exists() && !saveDefault(file)) {
            plugin.getLogger().log(Level.SEVERE, "Unable to create default config! Bailing.");
            return false;
        }

        Config parsedConfig;
        try {
            parsedConfig = new Config(file);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error while loading config file '" + file + "'", e);
            return false;
        }

        if (parsedConfig.authMultiplex.containsKey(0)) {
            plugin.getLogger().log(Level.SEVERE, "Attempted to register a multiplexer with id 0!");
            return false;
        }

        // Make sure there are no duplicate ports
        Set<Integer> registeredPorts = new HashSet<>();
        for (final var multiplexer : parsedConfig.authMultiplex.values()) {
            registeredPorts.add(multiplexer.port);
        }

        if (parsedConfig.authMultiplex.size() != registeredPorts.size()) {
            plugin.getLogger().log(Level.SEVERE, "Attempted to register multiple multiplexers on the same port!");
            return false;
        }

        multiplexerById.putAll(parsedConfig.authMultiplex);
        managedServers.putAll(parsedConfig.managedServers);

        return true;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean saveDefault(final File file) {
        try {
            file.getParentFile().mkdirs();
            Files.copy(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("config.toml")),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            );
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error while writing config file '" + file + "'", e);
            return false;
        }
    }

    @Nullable
    public Map.Entry<Integer, AuthMultiplex> getMultiplexerForPort(Integer port) {
        for (final var multiplexer : multiplexerById.entrySet()) {
            // We already checked there are no duplicate ports when parsing
            if (Objects.equals(multiplexer.getValue().port, port)) {
                return multiplexer;
            }
        }

        return null;
    }
}
