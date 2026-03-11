package org.oddlama.vane.core.command.argumentType

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import org.oddlama.vane.core.command.enums.WeatherValue
import java.util.concurrent.CompletableFuture

class WeatherArgumentType : CustomArgumentType.Converted<WeatherValue, String> {
    override fun getNativeType(): ArgumentType<String> = StringArgumentType.word()

    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: String): WeatherValue = WeatherValue.valueOf(nativeType.replaceFirstChar { it.uppercaseChar() })

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining.lowercase()
        WeatherValue.entries
            .filter { remaining.isBlank() || it.displayName.contains(remaining) }
            .forEach { builder.suggest(it.displayName) }
        return builder.buildFuture()
    }

    companion object {
        fun weather() = WeatherArgumentType()
    }
}
