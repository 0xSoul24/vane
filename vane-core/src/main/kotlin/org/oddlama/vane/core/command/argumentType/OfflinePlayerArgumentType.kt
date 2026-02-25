package org.oddlama.vane.core.command.argumentType

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.concurrent.CompletableFuture

class OfflinePlayerArgumentType : CustomArgumentType.Converted<OfflinePlayer, String> {
    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.word()
    }

    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: String): OfflinePlayer {
        for (p in Bukkit.getOfflinePlayers()) {
            if (nativeType.equals(p.name, ignoreCase = true)) {
                return p
            }
        }
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create()
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val players: Array<out OfflinePlayer> = Bukkit.getOfflinePlayers()
        var stream = players.asSequence().mapNotNull { it.name }
        if (builder.remaining.isNotBlank()) {
            stream = stream.filter { it.contains(builder.remaining, ignoreCase = true) }
        }
        stream.forEach { builder.suggest(it) }
        return builder.buildFuture()
    }

    companion object {
        @JvmStatic
        fun offlinePlayer(): OfflinePlayerArgumentType {
            return OfflinePlayerArgumentType()
        }
    }
}
