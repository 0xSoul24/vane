package org.oddlama.vane.admin.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

@Name("gamemode")
@Aliases({ "gm" })
public class Gamemode extends Command<Admin> {

    @LangMessage
    private TranslatedMessage langSet;

    public Gamemode(Context<Admin> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        return super.getCommandBase()
            .then(help())
            .executes(ctx -> {
                toggleGamemodeSelf((Player) ctx.getSource().getSender());
                return SINGLE_SUCCESS;
            })
            .then(
                argument("game_mode", ArgumentTypes.gameMode())
                    .executes(ctx -> {
                        setGamemodeSelf(
                            (Player) ctx.getSource().getSender(),
                            ctx.getArgument("game_mode", GameMode.class)
                        );
                        return SINGLE_SUCCESS;
                    })
                    .then(
                        argument("player", ArgumentTypes.player()).executes(ctx -> {
                            setGamemode(
                                ctx.getSource().getSender(),
                                ctx.getArgument("game_mode", GameMode.class),
                                player(ctx)
                            );
                            return SINGLE_SUCCESS;
                        })
                    )
            )
            .then(
                argument("player", ArgumentTypes.player()).executes(ctx -> {
                    toggleGamemodePlayer(ctx.getSource().getSender(), player(ctx));

                    return SINGLE_SUCCESS;
                })
            );
    }

    private Player player(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).get(0);
    }

    private void toggleGamemodeSelf(Player player) {
        toggleGamemodePlayer(player, player);
    }

    private void toggleGamemodePlayer(CommandSender sender, Player player) {
        setGamemode(sender, player.getGameMode() == GameMode.CREATIVE ? GameMode.SURVIVAL : GameMode.CREATIVE, player);
    }

    private void setGamemodeSelf(Player player, GameMode mode) {
        setGamemode(player, mode, player);
    }

    private void setGamemode(CommandSender sender, GameMode mode, Player player) {
        player.setGameMode(mode);
        langSet.send(
            sender,
            player.displayName().color(NamedTextColor.AQUA),
            Component.text(mode.name(), NamedTextColor.GREEN)
        );
    }
}
