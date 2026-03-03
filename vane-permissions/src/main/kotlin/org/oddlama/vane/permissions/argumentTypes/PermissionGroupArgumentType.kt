package org.oddlama.vane.permissions.argumentTypes

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream

class PermissionGroupArgumentType private constructor(var permissionGroups: MutableMap<String, MutableSet<String>>) :
    CustomArgumentType.Converted<String, String> {
    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: String): String {
        return nativeType
    }

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.string()
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        var stream: Stream<String> = permissionGroups.keys.stream()
        val remaining = builder.remaining
        if (!remaining.isBlank()) {
            stream = stream.filter { group: String -> group.contains(remaining) }
        }
        stream.forEach { text: String -> builder.suggest(text) }
        return builder.buildFuture()
    }

    companion object {
        fun permissionGroup(permissionGroups: MutableMap<String, MutableSet<String>>): PermissionGroupArgumentType {
            return PermissionGroupArgumentType(permissionGroups)
        }
    }
}
