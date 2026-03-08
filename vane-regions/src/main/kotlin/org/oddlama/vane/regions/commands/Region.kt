package org.oddlama.vane.regions.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.oddlama.vane.annotation.command.Aliases
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.regions.Regions

@Name("region")
@Aliases("regions", "rg")
class Region(context: Context<Regions?>) : org.oddlama.vane.core.command.Command<Regions?>(context) {
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .then(help())
            .requires { ctx: CommandSourceStack? -> ctx!!.sender is Player }
            .executes { ctx: CommandContext<CommandSourceStack?>? ->
                openMenu(ctx!!.getSource()!!.sender as Player)
                Command.SINGLE_SUCCESS
            }
    }

    private fun openMenu(player: Player) {
        module!!.menus?.mainMenu?.create(player)?.open(player)
    }
}
