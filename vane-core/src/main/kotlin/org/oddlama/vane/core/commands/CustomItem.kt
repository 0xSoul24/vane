package org.oddlama.vane.core.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.command.argumentType.CustomItemArgumentType
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.PlayerUtil.giveItem

@Name("customitem")
class CustomItem(context: Context<Core?>) : org.oddlama.vane.core.command.Command<Core?>(context) {
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        // Help
        return super.getCommandBase()
            .executes { stack: CommandContext<CommandSourceStack> ->
                printHelp2(stack)
                Command.SINGLE_SUCCESS
            }
            .then(help()) // Give custom item
            .then(
                Commands.literal("give")
                    .requires { stack: CommandSourceStack? -> stack!!.sender is Player }
                    .then(
                        Commands.argument("custom_item", CustomItemArgumentType.customItem(module!!))
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                val item = ctx.getArgument(
                                    "custom_item",
                                    CustomItem::class.java
                                )
                                giveCustomItem(ctx.getSource()!!.sender as Player, item)
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
    }

    private fun giveCustomItem(player: Player, customItem: CustomItem) {
        giveItem(player, customItem.newStack())
    }
}
