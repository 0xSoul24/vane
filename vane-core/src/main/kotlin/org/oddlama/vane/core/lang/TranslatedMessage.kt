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
    fun key(): String {
        return key
    }

    fun str(vararg args: Any?): String {
        try {
            val argsAsStrings = stringifyArgsForStr(key, args)
            return String.format(defaultTranslation, *argsAsStrings)
        } catch (e: Exception) {
            throw RuntimeException("Error while formatting message '$key'", e)
        }
    }

    fun strComponent(vararg args: Any?): Component {
        return LegacyComponentSerializer.legacySection().deserialize(str(*args))
    }

    fun format(vararg args: Any?): Component {
        if (module.core?.configClientSideTranslations != true) {
            return strComponent(*args)
        }

        val list = ArrayList<ComponentLike?>()
        for (o in args) {
            when (o) {
                is ComponentLike -> {
                    list.add(o)
                }

                is String -> {
                    list.add(LegacyComponentSerializer.legacySection().deserialize(o))
                }

                else -> {
                    throw RuntimeException("Error while formatting message '$key', got invalid argument $o")
                }
            }
        }
        return Component.translatable(key, list)
    }

    fun broadcastServerPlayers(vararg args: Any?) {
        val component = format(*args)
        for (player in module.server.onlinePlayers) {
            player.sendMessage(component)
        }
    }

    fun broadcastServer(vararg args: Any?) {
        val component = format(*args)
        for (player in module.server.onlinePlayers) {
            player.sendMessage(component)
        }
        module.clog.info(Component.text("[broadcast] ").append(strComponent(*args)))
    }

    fun broadcastWorld(world: World, vararg args: Any?) {
        val component = format(*args)
        for (player in world.players) {
            player.sendMessage(component)
        }
    }

    fun broadcastWorldActionBar(world: World, vararg args: Any?) {
        val component = format(*args)
        for (player in world.players) {
            player.sendActionBar(component)
        }
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

        // Also send it to sender if it's not the console
        if (sender != null && sender !== module.server.consoleSender) {
            sender.sendMessage(format(*args))
        }
    }
}
