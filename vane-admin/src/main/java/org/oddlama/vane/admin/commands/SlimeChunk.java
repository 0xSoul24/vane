package org.oddlama.vane.admin.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

@Name("slimechunk")
public class SlimeChunk extends Command<Admin> {

    @LangMessage
    private TranslatedMessage langSlimeChunkYes;

    @LangMessage
    private TranslatedMessage langSlimeChunkNo;

    public SlimeChunk(Context<Admin> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        return super.getCommandBase()
            .requires(stack -> stack.getSender() instanceof Player)
            .then(help())
            .executes(ctx -> {
                isSlimechunk((Player) ctx.getSource().getSender());
                return SINGLE_SUCCESS;
            });
    }

    private void isSlimechunk(final Player player) {
        if (player.getLocation().getChunk().isSlimeChunk()) {
            langSlimeChunkYes.send(player);
        } else {
            langSlimeChunkNo.send(player);
        }
    }
}
