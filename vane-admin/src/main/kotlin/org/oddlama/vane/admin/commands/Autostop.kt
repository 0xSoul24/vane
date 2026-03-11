package org.oddlama.vane.admin.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.command.CommandSender
import org.oddlama.vane.admin.Admin
import org.oddlama.vane.admin.AutostopGroup
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.util.ticksToMs

@Name("autostop")
class Autostop(var autostop: AutostopGroup) : org.oddlama.vane.core.command.Command<Admin?>(autostop) {
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .executes { ctx: CommandContext<CommandSourceStack> ->
                status(ctx.source.sender)
                Command.SINGLE_SUCCESS
            }
            .then(help())
            .then(
                Commands.literal("status").executes { ctx: CommandContext<CommandSourceStack> ->
                    status(ctx.source.sender)
                    Command.SINGLE_SUCCESS
                }
            )
            .then(
                Commands.literal("abort").executes { ctx: CommandContext<CommandSourceStack> ->
                    abort(ctx.source.sender)
                    Command.SINGLE_SUCCESS
                }
            )
            .then(
                Commands.literal("schedule")
                    .executes { ctx: CommandContext<CommandSourceStack> ->
                        schedule(ctx.source.sender)
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        Commands.argument("time", ArgumentTypes.time())
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                scheduleDelay(
                                    ctx.source.sender,
                                    ctx.getArgument("time", Int::class.java)
                                )
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
    }

    private fun status(sender: CommandSender?) {
        autostop.status(sender)
    }

    private fun abort(sender: CommandSender?) {
        autostop.abort(sender)
    }

    private fun schedule(sender: CommandSender?) {
        autostop.schedule(sender)
    }

    private fun scheduleDelay(sender: CommandSender?, delay: Int) {
        autostop.schedule(sender, ticksToMs(delay.toLong()))
    }
}
