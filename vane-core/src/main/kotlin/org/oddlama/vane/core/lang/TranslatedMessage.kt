package org.oddlama.vane.core.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.oddlama.vane.core.module.Module

class TranslatedMessage(
    private val module: Module<*>,
    private val key: String,
    private val defaultTranslation: String
) {
    fun key(): String = key

    fun str(vararg args: Any?): String =
        try {
            String.format(defaultTranslation, *stringifyArgsForStr(key, args))
        } catch (e: Exception) {
            throw RuntimeException("Error while formatting message '$key'", e)
        }

    fun strComponent(vararg args: Any?): Component =
        LegacyComponentSerializer.legacySection().deserialize(str(*args))

    fun format(vararg args: Any?): Component {
        if (module.core?.configClientSideTranslations != true) return strComponent(*args)
        val list = args.map { o ->
            when (o) {
                is ComponentLike -> o
                is String -> LegacyComponentSerializer.legacySection().deserialize(o)
                else -> throw RuntimeException("Error while formatting message '$key', got invalid argument $o")
            }
        }
        return Component.translatable(key, list)
    }

    fun broadcastServerPlayers(vararg args: Any?) {
        val component = format(*args)
        module.server.onlinePlayers.forEach { it.sendMessage(component) }
    }

    fun broadcastServer(vararg args: Any?) {
        val component = format(*args)
        module.server.onlinePlayers.forEach { it.sendMessage(component) }
        module.clog.info(Component.text("[broadcast] ").append(strComponent(*args)))
    }

    fun broadcastWorld(world: World, vararg args: Any?) {
        val component = format(*args)
        world.players.forEach { it.sendMessage(component) }
    }

    fun broadcastWorldActionBar(world: World, vararg args: Any?) {
        val component = format(*args)
        world.players.forEach { it.sendActionBar(component) }
    }

    fun send(sender: CommandSender?, vararg args: Any?) {
        if (sender == null || sender === module.server.consoleSender) {
            module.server.consoleSender.sendMessage(strComponent(*args))
        } else {
            sender.sendMessage(format(*args))
        }
    }

    fun sendActionBar(sender: CommandSender?, vararg args: Any?) {
        if (sender != null && sender !== module.server.consoleSender) {
            sender.sendActionBar(format(*args))
        }
    }

    fun sendAndLog(sender: CommandSender?, vararg args: Any?) {
        module.clog.info(strComponent(*args))
        if (sender != null && sender !== module.server.consoleSender) {
            sender.sendMessage(format(*args))
        }
    }
}
