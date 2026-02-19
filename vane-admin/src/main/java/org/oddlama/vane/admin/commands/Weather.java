package org.oddlama.vane.admin.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.argumentType.WeatherArgumentType;
import org.oddlama.vane.core.command.enums.WeatherValue;
import org.oddlama.vane.core.module.Context;

@Name("weather")
public class Weather extends Command<Admin> {

    public Weather(Context<Admin> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        return super.getCommandBase()
            .then(help())
            .then(
                argument("weather", WeatherArgumentType.weather())
                    .executes(ctx -> {
                        setWeatherCurrentWorld((Player) ctx.getSource().getSender(), weather(ctx));
                        return SINGLE_SUCCESS;
                    })
                    .then(
                        argument("world", ArgumentTypes.world()).executes(ctx -> {
                            setWeather(
                                ctx.getSource().getSender(),
                                weather(ctx),
                                ctx.getArgument("world", World.class)
                            );
                            return SINGLE_SUCCESS;
                        })
                    )
            );
    }

    private WeatherValue weather(CommandContext<CommandSourceStack> ctx) {
        return ctx.getArgument("weather", WeatherValue.class);
    }

    private void setWeatherCurrentWorld(Player player, WeatherValue w) {
        setWeather(player, w, player.getWorld());
    }

    private void setWeather(CommandSender sender, WeatherValue w, World world) {
        world.setStorm(w.storm());
        world.setThundering(w.thunder());
    }
}
