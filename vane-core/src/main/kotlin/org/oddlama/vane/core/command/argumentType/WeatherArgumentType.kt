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
    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.word()
    }

    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: String): WeatherValue {
        return WeatherValue.valueOf(nativeType)
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining
        for (weather in WeatherValue.entries) {
            val name = weather.name
            if (remaining.isBlank() || name.contains(remaining)) {
                builder.suggest(name)
            }
        }
        return builder.buildFuture()
    }

    companion object {
        @JvmStatic
        fun weather(): WeatherArgumentType {
            return WeatherArgumentType()
        }
    }
}
