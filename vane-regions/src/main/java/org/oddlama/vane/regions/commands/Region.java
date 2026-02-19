package org.oddlama.vane.regions.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.Regions;

@Name("region")
@Aliases({ "regions", "rg" })
public class Region extends Command<Regions> {

    public Region(Context<Regions> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        return super.getCommandBase()
            .then(help())
            .requires(ctx -> ctx.getSender() instanceof Player)
            .executes(ctx -> {
                openMenu((Player) ctx.getSource().getSender());
                return SINGLE_SUCCESS;
            });
    }

    private void openMenu(Player player) {
        getModule().menus.mainMenu.create(player).open(player);
    }
}
