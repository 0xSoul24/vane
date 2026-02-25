package org.oddlama.vane.core.command.argumentType

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.item.api.CustomItem
import java.util.concurrent.CompletableFuture

class CustomItemArgumentType private constructor(private var module: Core) :
    CustomArgumentType.Converted<CustomItem, NamespacedKey> {
    override fun getNativeType(): ArgumentType<NamespacedKey> {
        return ArgumentTypes.namespacedKey()
    }

    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: NamespacedKey): CustomItem {
        val items = module.itemRegistry()?.all() ?: emptyList()
        val found = items.filterNotNull().firstOrNull { item -> item.key() == nativeType }
        if (found != null) {
            return found
        }
        throw SimpleCommandExceptionType(
            MessageComponentSerializer.message().serialize(
                Component.text("Unknown custom item: $nativeType")
            )
        ).create()
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remainingLowerCase
        val items = module.itemRegistry()?.all() ?: emptyList()

        items
            .asSequence()
            .map { it?.key()?.toString() to it?.displayName() }
            .filter { remaining.isBlank() || it.first?.lowercase()?.contains(remaining) == true }
            .forEach { (key, name) ->
                if (name != null && key != null) {
                    builder.suggest(key, MessageComponentSerializer.message().serialize(name))
                } else if (key != null) {
                    builder.suggest(key)
                }
            }

        return builder.buildFuture()
    }

    companion object {
        fun customItem(module: Core): CustomItemArgumentType {
            return CustomItemArgumentType(module)
        }
    }
}
