package org.oddlama.vane.core.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.argumentType.EnchantmentFilterArgumentType;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

@Name("enchant")
public class Enchant extends Command<Core> {

    @LangMessage
    private TranslatedMessage langLevelTooLow;

    @LangMessage
    private TranslatedMessage langLevelTooHigh;

    @LangMessage
    private TranslatedMessage langInvalidEnchantment;

    public Enchant(Context<Core> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        return super.getCommandBase()
            .requires(ctx -> ctx.getSender() instanceof Player)
            .then(help())
            .then(
                argument("enchantment", EnchantmentFilterArgumentType.enchantmentFilter())
                    .executes(ctx -> {
                        enchantCurrentItemLevel1((Player) ctx.getSource().getSender(), enchantment(ctx));
                        return SINGLE_SUCCESS;
                    })
                    .then(
                        argument("level", IntegerArgumentType.integer(1)).executes(ctx -> {
                            enchantCurrentItem(
                                (Player) ctx.getSource().getSender(),
                                enchantment(ctx),
                                ctx.getArgument("level", Integer.class)
                            );
                            return SINGLE_SUCCESS;
                        })
                    )
            );
    }

    private Enchantment enchantment(CommandContext<CommandSourceStack> ctx) {
        return ctx.getArgument("enchantment", Enchantment.class);
    }

    private boolean filterByHeldItem(CommandSender sender, Enchantment e) {
        if (!(sender instanceof Player)) {
            return false;
        }

        final var player = (Player) sender;
        final var itemStack = player.getEquipment().getItemInMainHand();
        boolean isBook = itemStack.getType() == Material.BOOK || itemStack.getType() == Material.ENCHANTED_BOOK;
        return isBook || e.canEnchantItem(itemStack);
    }

    private void enchantCurrentItemLevel1(Player player, Enchantment enchantment) {
        enchantCurrentItem(player, enchantment, 1);
    }

    private void enchantCurrentItem(Player player, Enchantment enchantment, Integer level) {
        if (level < enchantment.getStartLevel()) {
            langLevelTooLow.send(player, "§b" + level, "§a" + enchantment.getStartLevel());
            return;
        } else if (level > enchantment.getMaxLevel()) {
            langLevelTooHigh.send(player, "§b" + level, "§a" + enchantment.getMaxLevel());
            return;
        }

        var itemStack = player.getEquipment().getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
            langInvalidEnchantment.send(player, "§b" + enchantment.getKey(), "§a" + itemStack.getType().getKey());
            return;
        }

        try {
            // Convert a book if necessary
            if (itemStack.getType() == Material.BOOK) {
                // FIXME this technically yields wrong items when this was a tome,
                // as just changing the base item is not equivalent to custom item conversion.
                // The custom model data and item tag will still be those of a book.
                // The fix is not straightforward without hardcoding tome identifiers,
                // so for now we leave it as is.
                itemStack = itemStack.withType(Material.ENCHANTED_BOOK);
                /* fallthrough */
            }

            if (itemStack.getType() == Material.ENCHANTED_BOOK) {
                final var meta = (EnchantmentStorageMeta) itemStack.getItemMeta();
                meta.addStoredEnchant(enchantment, level, false);
                itemStack.setItemMeta(meta);
            } else {
                itemStack.addEnchantment(enchantment, level);
            }

            getModule().enchantmentManager.updateEnchantedItem(itemStack);
        } catch (Exception e) {
            langInvalidEnchantment.send(player, "§b" + enchantment.getKey(), "§a" + itemStack.getType().getKey());
        }
    }
}
