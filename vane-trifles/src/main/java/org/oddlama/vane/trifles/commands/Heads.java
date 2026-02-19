package org.oddlama.vane.trifles.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static org.oddlama.vane.util.PlayerUtil.giveItems;
import static org.oddlama.vane.util.PlayerUtil.takeItems;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.PlayerUtil;

@Name("heads")
public class Heads extends Command<Trifles> {

    @ConfigMaterial(def = Material.BONE, desc = "Currency material used to buy heads.")
    public Material configCurrency;

    @ConfigInt(def = 1, min = 0, desc = "Price (in currency) per head. Set to 0 for free heads.")
    public int configPricePerHead;

    public Heads(Context<Trifles> context) {
        // Anyone may use this by default.
        super(context, PermissionDefault.TRUE);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        return super.getCommandBase()
            .requires(ctx -> ctx.getSender() instanceof Player)
            .then(help())
            .executes(ctx -> {
                openHeadLibrary((Player) ctx.getSource().getSender());
                return SINGLE_SUCCESS;
            });
    }

    private void openHeadLibrary(final Player player) {
        MenuFactory.headSelector(
            getContext(),
            player,
            (player2, m, t, event) -> {
                final int amount;
                switch (event.getClick()) {
                    default:
                        return ClickResult.INVALID_CLICK;
                    case NUMBER_KEY:
                        amount = event.getHotbarButton() + 1;
                        break;
                    case LEFT:
                        amount = 1;
                        break;
                    case RIGHT:
                        amount = 32;
                        break;
                    case MIDDLE:
                    case SHIFT_LEFT:
                        amount = 64;
                        break;
                    case SHIFT_RIGHT:
                        amount = 16;
                        break;
                }

                // Take currency items
                if (
                    configPricePerHead > 0 &&
                    !PlayerUtil.takeItems(player2, new ItemStack(configCurrency, configPricePerHead * amount))
                ) {
                    return ClickResult.ERROR;
                }

                giveItems(player2, t.item(), amount);
                return ClickResult.SUCCESS;
            },
            player2 -> {}
        ).open(player);
    }
}
