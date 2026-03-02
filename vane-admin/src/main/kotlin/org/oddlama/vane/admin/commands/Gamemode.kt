package org.oddlama.vane.admin.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.oddlama.vane.admin.Admin
import org.oddlama.vane.annotation.command.Aliases
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context

@Name("gamemode")
@Aliases("gm")
class Gamemode(context: Context<Admin?>) : org.oddlama.vane.core.command.Command<Admin?>(context) {
    @LangMessage
    private val langSet: TranslatedMessage? = null

    // Cambiado CommandSourceStack? -> CommandSourceStack (no-null)
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .then(help())
            .executes { ctx: CommandContext<CommandSourceStack> ->
                toggleGamemodeSelf(ctx.source.sender as Player)
                Command.SINGLE_SUCCESS
            }
            .then(
                // Usar tipos no-null para los argumentos genéricos
                Commands.argument("game_mode", ArgumentTypes.gameMode())
                    .executes { ctx: CommandContext<CommandSourceStack> ->
                        setGamemodeSelf(
                            ctx.source.sender as Player,
                            ctx.getArgument("game_mode", GameMode::class.java)
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        Commands.argument("player", ArgumentTypes.player())
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                setGamemode(
                                    ctx.source.sender,
                                    ctx.getArgument("game_mode", GameMode::class.java),
                                    player(ctx)
                                )
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
            .then(
                Commands.argument("player", ArgumentTypes.player())
                    .executes { ctx: CommandContext<CommandSourceStack> ->
                        toggleGamemodePlayer(ctx.source.sender, player(ctx))
                        Command.SINGLE_SUCCESS
                    }
            )
    }

    @Throws(CommandSyntaxException::class)
    private fun player(ctx: CommandContext<CommandSourceStack>): Player {
        return ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
            .resolve(ctx.source)[0]
    }

    private fun toggleGamemodeSelf(player: Player) {
        toggleGamemodePlayer(player, player)
    }

    private fun toggleGamemodePlayer(sender: CommandSender?, player: Player) {
        setGamemode(
            sender,
            if (player.gameMode == GameMode.CREATIVE) GameMode.SURVIVAL else GameMode.CREATIVE,
            player
        )
    }

    private fun setGamemodeSelf(player: Player, mode: GameMode) {
        setGamemode(player, mode, player)
    }

    private fun setGamemode(sender: CommandSender?, mode: GameMode, player: Player) {
        player.gameMode = mode
        langSet!!.send(
            sender,
            player.displayName().color(NamedTextColor.AQUA),
            Component.text(mode.name, NamedTextColor.GREEN)
        )
    }
}
