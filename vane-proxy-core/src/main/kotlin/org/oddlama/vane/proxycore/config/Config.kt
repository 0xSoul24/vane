package org.oddlama.vane.proxycore.config

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.file.CommentedFileConfig
import java.io.File

/**
 * Parsed proxy-core TOML configuration model.
 *
 * @constructor Loads and validates the configuration from [file].
 */
class Config(file: File) {
    // multiplexerId, { Integer port, List<UUID> allowed_uuids }
    /** Auth multiplexers by configured numeric identifier. */
    @JvmField
    var authMultiplex: LinkedHashMap<Int?, AuthMultiplex?>?

    /** Managed backend server configurations by server id. */
    @JvmField
    var managedServers: LinkedHashMap<String?, ManagedServer?>?

    init {
        val config = CommentedFileConfig.builder(file)
            .autosave()
            .preserveInsertionOrder()
            .sync()
            .build()

        config.load()

        val authMultiplex = linkedMapOf<Int?, AuthMultiplex?>()
        val managedServers = linkedMapOf<String?, ManagedServer?>()

        val registeredPorts = mutableSetOf<Int?>()
        val multiplexersConfig = config.get<CommentedConfig>("auth_multiplex")
        for (multiplexerConf in multiplexersConfig.entrySet()) {
            val keyString = multiplexerConf.key
            val key = keyString.toIntOrNull()
                ?: throw IllegalArgumentException("Multiplexer ID '$keyString' is not an integer!")

            val value = multiplexerConf.getValue<Any?>()
            require(value is CommentedConfig) { "Multiplexer '$key' has an invalid configuration!" }

            val port = value.getInt("port")
            require(port !in registeredPorts) { "Multiplexer ID '$keyString' uses an already registered port!" }

            val multiplexer = AuthMultiplex(port, value.get<MutableList<String?>?>("allowed_uuids"))

            registeredPorts.add(multiplexer.port)
            authMultiplex[key] = multiplexer
        }

        this.authMultiplex = authMultiplex

        val serversConfig = config.get<CommentedConfig>("managed_servers")
        for (serverConf in serversConfig.entrySet()) {
            val key = serverConf.key

            val value = serverConf.getValue<Any?>()
            require(value is CommentedConfig) { "Managed server '$key' has an invalid configuration!" }

            val managedServer = ManagedServer(
                key,
                value.get("displayName"),
                value.get("online"),
                value.get("offline"),
                value.get("start")
            )

            managedServers[key] = managedServer
        }

        this.managedServers = managedServers
    }
}
