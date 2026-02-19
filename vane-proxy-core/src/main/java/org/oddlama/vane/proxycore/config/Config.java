package org.oddlama.vane.proxycore.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class Config {

    // multiplexerId, { Integer port, List<UUID> allowed_uuids }
    public LinkedHashMap<Integer, AuthMultiplex> authMultiplex;
    public LinkedHashMap<String, ManagedServer> managedServers;

    public Config(File file) throws IOException {
        CommentedFileConfig config = CommentedFileConfig.builder(file)
            .autosave()
            .preserveInsertionOrder()
            .sync()
            .build();

        config.load();

        LinkedHashMap<Integer, AuthMultiplex> authMultiplex = new LinkedHashMap<>();
        LinkedHashMap<String, ManagedServer> managedServers = new LinkedHashMap<>();

        Set<Integer> registeredPorts = new HashSet<>();
        CommentedConfig multiplexersConfig = config.get("auth_multiplex");
        for (final var multiplexerConf : multiplexersConfig.entrySet()) {
            final var keyString = multiplexerConf.getKey();
            int key;
            try {
                key = Integer.parseInt(keyString);
            } catch (Exception ignored) {
                throw new IllegalArgumentException("Multiplexer ID '" + keyString + "' is not an integer!");
            }

            final var value = multiplexerConf.getValue();
            if (!(value instanceof final CommentedConfig multiplexerConfig)) throw new IllegalArgumentException(
                "Multiplexer '" + key + "' has an invalid configuration!"
            );

            final var port = multiplexerConfig.getInt("port");
            if (registeredPorts.contains(port)) throw new IllegalArgumentException(
                "Multiplexer ID '" + keyString + "' uses an already registered port!"
            );

            final var multiplexer = new AuthMultiplex(port, multiplexerConfig.get("allowed_uuids"));

            registeredPorts.add(multiplexer.port);
            authMultiplex.put(key, multiplexer);
        }

        this.authMultiplex = authMultiplex;

        CommentedConfig serversConfig = config.get("managed_servers");
        for (final var serverConf : serversConfig.entrySet()) {
            final var key = serverConf.getKey();

            final var value = serverConf.getValue();
            if (!(value instanceof final CommentedConfig managedServerConfig)) throw new IllegalArgumentException(
                "Managed server '" + key + "' has an invalid configuration!"
            );

            final var managedServer = new ManagedServer(
                key,
                managedServerConfig.get("displayName"),
                managedServerConfig.get("online"),
                managedServerConfig.get("offline"),
                managedServerConfig.get("start")
            );

            managedServers.put(key, managedServer);
        }

        this.managedServers = managedServers;
    }
}
