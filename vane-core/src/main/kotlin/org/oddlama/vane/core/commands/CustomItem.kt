package org.oddlama.vane.core.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
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
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> =
        super.getCommandBase()
            .executes { stack ->
                printHelp2(stack)
                Command.SINGLE_SUCCESS
            }
            .then(help())
            .then(
                Commands.literal("give")
                    .requires { it.sender is Player }
                    .then(
                        Commands.argument("custom_item", CustomItemArgumentType.customItem(module!!))
                            .executes { ctx ->
                                val item = ctx.getArgument("custom_item", CustomItem::class.java)
                                giveItem(ctx.source.sender as Player, item.newStack())
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
}

