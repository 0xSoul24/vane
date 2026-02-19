package org.oddlama.vane.admin.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static org.oddlama.vane.util.WorldUtil.changeTimeSmoothly;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.argumentType.TimeValueArgumentType;
import org.oddlama.vane.core.command.enums.TimeValue;
import org.oddlama.vane.core.module.Context;

@Name("time")
public class Time extends Command<Admin> {

    public Time(Context<Admin> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        return super.getCommandBase()
            .then(help())
            .then(
                argument("time", TimeValueArgumentType.timeValue())
                    .executes(ctx -> {
                        setTimeCurrentWorld((Player) ctx.getSource().getSender(), timeValue(ctx));
                        return SINGLE_SUCCESS;
                    })
                    .then(
                        argument("world", ArgumentTypes.world()).executes(ctx -> {
                            setTime(timeValue(ctx), ctx.getArgument("world", World.class));
                            return SINGLE_SUCCESS;
                        })
                    )
            );
    }

    private TimeValue timeValue(CommandContext<CommandSourceStack> ctx) {
        return ctx.getArgument("time", TimeValue.class);
    }

    private void setTimeCurrentWorld(Player player, TimeValue t) {
        changeTimeSmoothly(player.getWorld(), getModule(), t.ticks(), 100);
    }

    private void setTime(TimeValue t, World world) {
        changeTimeSmoothly(world, getModule(), t.ticks(), 100);
    }
}
