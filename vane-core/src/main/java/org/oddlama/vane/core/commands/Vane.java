package org.oddlama.vane.core.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Random;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.argumentType.ModuleArgumentType;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;

@Name("vane")
public class Vane extends Command<Core> {

    @LangMessage
    private TranslatedMessage langReloadSuccess;

    @LangMessage
    private TranslatedMessage langReloadFail;

    @LangMessage
    private TranslatedMessage langResourcePackGenerateSuccess;

    @LangMessage
    private TranslatedMessage langResourcePackGenerateFail;

    public Vane(Context<Core> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        return super.getCommandBase()
            .then(help())
            .then(
                literal("reload")
                    .executes(ctx -> {
                        reloadAll(ctx.getSource().getSender());
                        return SINGLE_SUCCESS;
                    })
                    .then(
                        argument("module", ModuleArgumentType.module(getModule())).executes(ctx -> {
                            reloadModule(ctx.getSource().getSender(), ctx.getArgument("module", Module.class));
                            return SINGLE_SUCCESS;
                        })
                    )
            )
            .then(
                literal("generate_resource_pack").executes(ctx -> {
                    generateResourcePack(ctx.getSource().getSender());
                    return SINGLE_SUCCESS;
                })
            )
            .then(
                literal("test_do_not_use_if_you_are_not_a_dev").executes(ctx -> {
                    test(ctx.getSource().getSender());
                    return SINGLE_SUCCESS;
                })
            );
    }

    private void reloadModule(CommandSender sender, Module<?> module) {
        if (module.reloadConfiguration()) {
            langReloadSuccess.send(sender, "§bvane-" + module.getAnnotationName());
        } else {
            langReloadFail.send(sender, "§bvane-" + module.getAnnotationName());
        }
    }

    private void reloadAll(CommandSender sender) {
        for (var m : getModule().core.getModules()) {
            reloadModule(sender, m);
        }
    }

    private void generateResourcePack(CommandSender sender) {
        var file = getModule().generateResourcePack();
        if (file != null) {
            langResourcePackGenerateSuccess.send(sender, file.getAbsolutePath());
        } else {
            langResourcePackGenerateFail.send(sender);
        }
        if (sender instanceof Player) {
            var dist = getModule().resourcePackDistributor;
            dist.updateSha1(file);
            dist.sendResourcePack((Player) sender);
        }
    }

    private void testTomeGeneration() {
        final var lootTable = LootTables.ABANDONED_MINESHAFT.getLootTable();
        final var inventory = getModule().getServer().createInventory(null, 3 * 9);
        final var context =
            (new LootContext.Builder(
                    getModule().getServer().getWorlds().get(0).getBlockAt(0, 0, 0).getLocation()
                )).build();
        final var random = new Random();

        int tomes = 0;
        final var simulationCount = 10000;
        final var gtPercentage = 0.2; // (0-2) (average 1) with 1/5 chance
        final var tolerance = 0.7;
        getModule().log.info("Testing ancient tome generation...");

        for (int i = 0; i < simulationCount; ++i) {
            inventory.clear();
            lootTable.fillInventory(inventory, random, context);
            for (final var is : inventory.getStorageContents()) {
                if (is != null && is.hasItemMeta()) {
                    final var modelData = is.getItemMeta().getCustomModelDataComponent().getFloats();
                    if (!modelData.isEmpty() && modelData.getFirst() == 0x770000) {
                        ++tomes;
                    }
                }
            }
        }

        if (tomes == 0) {
            getModule().log.severe("0 tomes were generated in " + simulationCount + " chests.");
        } else if (
            tomes > gtPercentage * simulationCount * tolerance &&
            tomes < (gtPercentage * simulationCount) / tolerance
        ) { // 70% tolerance to lower bound
            getModule()
                .log.warning(
                    tomes +
                    " tomes were generated in " +
                    simulationCount +
                    " chests. This is " +
                    ((100.0 * ((double) tomes / simulationCount)) / gtPercentage) +
                    "% of the expected value."
                );
        } else {
            getModule()
                .log.info(
                    tomes +
                    " tomes were generated in " +
                    simulationCount +
                    " chests. This is " +
                    ((100.0 * ((double) tomes / simulationCount)) / gtPercentage) +
                    "% of the expected value."
                );
        }
    }

    private void test(CommandSender sender) {
        testTomeGeneration();
    }
}
