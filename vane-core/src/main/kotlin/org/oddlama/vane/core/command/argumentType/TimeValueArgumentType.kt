package org.oddlama.vane.core.command.argumentType

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import org.oddlama.vane.core.command.enums.TimeValue
import java.util.concurrent.CompletableFuture

class TimeValueArgumentType : CustomArgumentType.Converted<TimeValue, String> {
    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.word()
    }

    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: String): TimeValue {
        return TimeValue.valueOf(nativeType)
    }

    // Use the exact signature expected by Brigadier: S : Any
    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining
        for (time in TimeValue.entries) {
            val name = time.name
            if (remaining.isBlank() || name.contains(remaining)) {
                builder.suggest(name)
            }
        }
        return builder.buildFuture()
    }

    companion object {
        @JvmStatic
        fun timeValue(): TimeValueArgumentType {
            return TimeValueArgumentType()
        }
    }
}
