package org.oddlama.vane.admin.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.oddlama.vane.admin.Admin
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context

@Name("slimechunk")
class SlimeChunk(context: Context<Admin?>) : org.oddlama.vane.core.command.Command<Admin?>(context) {
    @LangMessage
    private val langSlimeChunkYes: TranslatedMessage? = null

    @LangMessage
    private val langSlimeChunkNo: TranslatedMessage? = null

    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .requires { stack: CommandSourceStack -> stack.sender is Player }
            .then(help())
            .executes { ctx: CommandContext<CommandSourceStack> ->
                isSlimechunk(ctx.source.sender as Player)
                Command.SINGLE_SUCCESS
            }
    }

    private fun isSlimechunk(player: Player) {
        if (player.location.chunk.isSlimeChunk) {
            langSlimeChunkYes!!.send(player)
        } else {
            langSlimeChunkNo!!.send(player)
        }
    }
}
