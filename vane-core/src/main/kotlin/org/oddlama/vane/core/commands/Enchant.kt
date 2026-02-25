package org.oddlama.vane.core.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.command.argumentType.EnchantmentFilterArgumentType.Companion.enchantmentFilter
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context

@Name("enchant")
class Enchant(context: Context<Core?>) : org.oddlama.vane.core.command.Command<Core?>(context) {
    @LangMessage
    private val langLevelTooLow: TranslatedMessage? = null

    @LangMessage
    private val langLevelTooHigh: TranslatedMessage? = null

    @LangMessage
    private val langInvalidEnchantment: TranslatedMessage? = null

    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .requires { ctx: CommandSourceStack? -> ctx!!.sender is Player }
            .then(help())
            .then(
                Commands.argument("enchantment", enchantmentFilter())
                    .executes { ctx: CommandContext<CommandSourceStack> ->
                        enchantCurrentItemLevel1(ctx.getSource()!!.sender as Player, enchantment(ctx)!!)
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        Commands.argument("level", IntegerArgumentType.integer(1))
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                enchantCurrentItem(
                                    ctx.getSource()!!.sender as Player,
                                    enchantment(ctx)!!,
                                    ctx.getArgument("level", Int::class.java)
                                )
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
    }

    private fun enchantment(ctx: CommandContext<CommandSourceStack>): Enchantment? {
        return ctx.getArgument("enchantment", Enchantment::class.java)
    }

    private fun filterByHeldItem(sender: CommandSender?, e: Enchantment): Boolean {
        if (sender !is Player) {
            return false
        }

        val itemStack = sender.equipment.itemInMainHand
        val isBook = itemStack.type == Material.BOOK || itemStack.type == Material.ENCHANTED_BOOK
        return isBook || e.canEnchantItem(itemStack)
    }

    private fun enchantCurrentItemLevel1(player: Player, enchantment: Enchantment) {
        enchantCurrentItem(player, enchantment, 1)
    }

    private fun enchantCurrentItem(player: Player, enchantment: Enchantment, level: Int) {
        if (level < enchantment.startLevel) {
            langLevelTooLow!!.send(player, "§b$level", "§a" + enchantment.startLevel)
            return
        } else if (level > enchantment.maxLevel) {
            langLevelTooHigh!!.send(player, "§b$level", "§a" + enchantment.maxLevel)
            return
        }

        var itemStack = player.equipment.itemInMainHand
        if (itemStack.type == Material.AIR) {
            langInvalidEnchantment!!.send(player, "§b" + enchantment.key, "§a" + itemStack.type.getKey())
            return
        }

        try {
            // Convert a book if necessary
            if (itemStack.type == Material.BOOK) {
                // FIXME this technically yields wrong items when this was a tome,
                // as just changing the base item is not equivalent to custom item conversion.
                // The custom model data and item tag will still be those of a book.
                // The fix is not straightforward without hardcoding tome identifiers,
                // so for now we leave it as is.
                itemStack = itemStack.withType(Material.ENCHANTED_BOOK)
                /* fallthrough */
            }

            if (itemStack.type == Material.ENCHANTED_BOOK) {
                val meta = itemStack.itemMeta as EnchantmentStorageMeta
                meta.addStoredEnchant(enchantment, level, false)
                itemStack.setItemMeta(meta)
            } else {
                itemStack.addEnchantment(enchantment, level)
            }

            // Use safe-call in case enchantmentManager is nullable
            module!!.enchantmentManager?.updateEnchantedItem(itemStack)
        } catch (e: Exception) {
            langInvalidEnchantment!!.send(player, "§b" + enchantment.key, "§a" + itemStack.type.getKey())
        }
    }
}
