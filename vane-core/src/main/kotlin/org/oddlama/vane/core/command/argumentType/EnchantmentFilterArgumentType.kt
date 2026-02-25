package org.oddlama.vane.core.command.argumentType

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class EnchantmentFilterArgumentType : CustomArgumentType.Converted<Enchantment, Enchantment> {

    override fun getNativeType(): ArgumentType<Enchantment> =
        ArgumentTypes.resource(RegistryKey.ENCHANTMENT)

    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: Enchantment): Enchantment = nativeType

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val stack = context.source as CommandSourceStack
        val item = (stack.sender as Player).inventory.itemInMainHand

        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)

        var enchantments = registry.asSequence()
        if (item.type != Material.BOOK && item.type != Material.ENCHANTED_BOOK) {
            enchantments = enchantments.filter { it.canEnchantItem(item) }
        }

        var suggestions = enchantments.map { it.key.asString() }
        if (builder.remaining.isNotBlank()) {
            suggestions = suggestions.filter { it.contains(builder.remainingLowerCase) }
        }

        suggestions.forEach { builder.suggest(it) }
        return builder.buildFuture()
    }

    companion object {
        @JvmStatic
        fun enchantmentFilter(): EnchantmentFilterArgumentType = EnchantmentFilterArgumentType()
    }
}
