package org.oddlama.vane.admin.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.oddlama.vane.admin.Admin
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.core.command.argumentType.WeatherArgumentType
import org.oddlama.vane.core.command.enums.WeatherValue
import org.oddlama.vane.core.module.Context

@Name("weather")
class Weather(context: Context<Admin?>) : org.oddlama.vane.core.command.Command<Admin?>(context) {
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> =
        super.getCommandBase()
            .then(help())
            .then(
                Commands.argument<WeatherValue>("weather", WeatherArgumentType.weather())
                    .executes { ctx ->
                        setWeatherCurrentWorld(ctx.source.sender as Player, weather(ctx))
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        Commands.argument("world", ArgumentTypes.world())
                            .executes { ctx ->
                                setWeather(
                                    ctx.source.sender,
                                    weather(ctx),
                                    ctx.getArgument("world", World::class.java)
                                )
                                Command.SINGLE_SUCCESS
                            }
                    )
            )

    private fun weather(ctx: CommandContext<CommandSourceStack>): WeatherValue =
        ctx.getArgument("weather", WeatherValue::class.java)

    private fun setWeatherCurrentWorld(player: Player, w: WeatherValue) =
        setWeather(player, w, player.world)

    private fun setWeather(@Suppress("UNUSED_PARAMETER") sender: CommandSender?, w: WeatherValue, world: World) {
        world.setStorm(w.storm)
        world.isThundering = w.thunder
    }
}
