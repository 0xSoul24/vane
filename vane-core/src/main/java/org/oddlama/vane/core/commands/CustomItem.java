package org.oddlama.vane.core.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.argumentType.CustomItemArgumentType;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.PlayerUtil;

@Name("customitem")
public class CustomItem extends Command<Core> {

    public CustomItem(Context<Core> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        // Help
        return super.getCommandBase()
            .executes(stack -> {
                printHelp2(stack);
                return SINGLE_SUCCESS;
            })
            .then(help())
            // Give custom item
            .then(
                literal("give")
                    .requires(stack -> stack.getSender() instanceof Player)
                    .then(
                        argument("custom_item", CustomItemArgumentType.customItem(getModule())).executes(ctx -> {
                            org.oddlama.vane.core.item.api.CustomItem item = ctx.getArgument(
                                "custom_item",
                                org.oddlama.vane.core.item.api.CustomItem.class
                            );
                            giveCustomItem((Player) ctx.getSource().getSender(), item);
                            return SINGLE_SUCCESS;
                        })
                    )
            );
    }

    private void giveCustomItem(final Player player, final org.oddlama.vane.core.item.api.CustomItem customItem) {
        PlayerUtil.giveItem(player, customItem.newStack());
    }
}
