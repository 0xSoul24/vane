package org.oddlama.vane.core.command.argumentType

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.module.Module
import net.kyori.adventure.text.Component
import java.util.concurrent.CompletableFuture

class ModuleArgumentType : CustomArgumentType.Converted<Module<*>, String> {
    private lateinit var core: Core

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.word()
    }

    private fun setCore(core: Core): ModuleArgumentType {
        this.core = core
        return this
    }

    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: String): Module<*> {
        val found = core.modules
            .asSequence()
            .filterNotNull()
            .firstOrNull { it.annotationName.equals(nativeType, ignoreCase = true) }

        if (found != null) return found

        throw SimpleCommandExceptionType(
            MessageComponentSerializer.message().serialize(
                Component.text("Unknown module: $nativeType")
            )
        ).create()
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remainingLowerCase
        core.modules
            .asSequence()
            .filterNotNull()
            .map { it.annotationName.lowercase() }
            .filter { remaining.isBlank() || it.contains(remaining) }
            .forEach { builder.suggest(it) }
        return builder.buildFuture()
    }

    companion object {
        @JvmStatic
        fun module(core: Core): ModuleArgumentType {
            return ModuleArgumentType().setCore(core)
        }
    }
}
