package org.oddlama.vane.proxycore.config

import org.oddlama.vane.proxycore.VaneProxyPlugin
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.logging.Level

class ConfigManager(private val plugin: VaneProxyPlugin) {
    // name → managed server
    @JvmField
    val managedServers: MutableMap<String?, ManagedServer?> = HashMap<String?, ManagedServer?>()

    // port → alias id (starts at 1)
    @JvmField
    val multiplexerById: MutableMap<Int?, AuthMultiplex?> = HashMap<Int?, AuthMultiplex?>()

    private fun file(): File {
        return File(plugin.dataFolder, "config.toml")
    }

    fun load(): Boolean {
        val file = file()
        if (!file.exists() && !saveDefault(file)) {
            plugin.getLogger().log(Level.SEVERE, "Unable to create default config! Bailing.")
            return false
        }

        val parsedConfig: Config?
        try {
            parsedConfig = Config(file)
        } catch (e: Exception) {
            plugin.getLogger().log(Level.SEVERE, "Error while loading config file '$file'", e)
            return false
        }

        if (parsedConfig.authMultiplex!!.containsKey(0)) {
            plugin.getLogger().log(Level.SEVERE, "Attempted to register a multiplexer with id 0!")
            return false
        }

        // Make sure there are no duplicate ports
        val registeredPorts: MutableSet<Int?> = HashSet<Int?>()
        for (multiplexer in parsedConfig.authMultiplex!!.values) {
            registeredPorts.add(multiplexer!!.port)
        }

        if (parsedConfig.authMultiplex!!.size != registeredPorts.size) {
            plugin.getLogger().log(Level.SEVERE, "Attempted to register multiple multiplexers on the same port!")
            return false
        }

        multiplexerById.putAll(parsedConfig.authMultiplex!!)
        managedServers.putAll(parsedConfig.managedServers!!)

        return true
    }

    fun saveDefault(file: File): Boolean {
        try {
            file.getParentFile().mkdirs()
            Files.copy(
                Objects.requireNonNull(javaClass.getClassLoader().getResourceAsStream("config.toml")),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            return true
        } catch (e: Exception) {
            plugin.getLogger().log(Level.SEVERE, "Error while writing config file '$file'", e)
            return false
        }
    }

    fun getMultiplexerForPort(port: Int?): MutableMap.MutableEntry<Int?, AuthMultiplex?>? {
        for (multiplexer in multiplexerById.entries) {
            // We already checked there are no duplicate ports when parsing
            if (multiplexer.value!!.port == port) {
                return multiplexer
            }
        }

        return null
    }
}
