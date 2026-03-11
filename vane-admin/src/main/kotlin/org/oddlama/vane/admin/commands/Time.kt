package org.oddlama.vane.admin.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.World
import org.bukkit.entity.Player
import org.oddlama.vane.admin.Admin
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.core.command.argumentType.TimeValueArgumentType
import org.oddlama.vane.core.command.enums.TimeValue
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.WorldUtil

@Name("time")
class Time(context: Context<Admin?>) : org.oddlama.vane.core.command.Command<Admin?>(context) {
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> =
        super.getCommandBase()
            .then(help())
            .then(
                Commands.argument<TimeValue>("time", TimeValueArgumentType.timeValue())
                    .executes { ctx ->
                        setTimeCurrentWorld(ctx.source.sender as Player, timeValue(ctx))
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        Commands.argument("world", ArgumentTypes.world())
                            .executes { ctx ->
                                setTime(timeValue(ctx), ctx.getArgument("world", World::class.java))
                                Command.SINGLE_SUCCESS
                            }
                    )
            )

    private fun timeValue(ctx: CommandContext<CommandSourceStack>): TimeValue =
        ctx.getArgument("time", TimeValue::class.java)

    private fun setTimeCurrentWorld(player: Player, t: TimeValue) =
        WorldUtil.changeTimeSmoothly(player.world, module!!, t.ticks.toLong(), 100)

    private fun setTime(t: TimeValue, world: World) =
        WorldUtil.changeTimeSmoothly(world, module!!, t.ticks.toLong(), 100)
}
