package org.oddlama.vane.core.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.loot.LootContext
import org.bukkit.loot.LootTables
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.command.argumentType.ModuleArgumentType
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import java.util.*

@Name("vane")
class Vane(context: Context<Core?>) : org.oddlama.vane.core.command.Command<Core?>(context) {
    @LangMessage
    private val langReloadSuccess: TranslatedMessage? = null

    @LangMessage
    private val langReloadFail: TranslatedMessage? = null

    @LangMessage
    private val langResourcePackGenerateSuccess: TranslatedMessage? = null

    @LangMessage
    private val langResourcePackGenerateFail: TranslatedMessage? = null

    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .then(help())
            .then(
                Commands.literal("reload")
                    .executes { ctx: CommandContext<CommandSourceStack> ->
                        reloadAll(ctx.getSource()!!.sender)
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        Commands.argument<Module<*>>("module", ModuleArgumentType.module(module!!))
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                reloadModule(
                                    ctx.getSource()!!.sender,
                                    // Cast to Module<*> to satisfy Kotlin's type system for captured generics
                                    ctx.getArgument("module", Module::class.java) as Module<*>
                                )
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
            .then(
                Commands.literal("generate_resource_pack")
                    .executes { ctx: CommandContext<CommandSourceStack> ->
                        generateResourcePack(ctx.getSource()!!.sender)
                        Command.SINGLE_SUCCESS
                    }
            )
            .then(
                Commands.literal("test_do_not_use_if_you_are_not_a_dev")
                    .executes { ctx: CommandContext<CommandSourceStack> ->
                        test(ctx.getSource()!!.sender)
                        Command.SINGLE_SUCCESS
                    }
            )
    }

    private fun reloadModule(sender: CommandSender?, module: Module<*>) {
        if (module.reloadConfiguration()) {
            langReloadSuccess!!.send(sender, "§bvane-" + module.annotationName)
        } else {
            langReloadFail!!.send(sender, "§bvane-" + module.annotationName)
        }
    }

    private fun reloadAll(sender: CommandSender?) {
        // Iterate modules from core if available, otherwise nothing to reload
        val coreModules = module!!.core?.modules?.filterNotNull() ?: emptySet()
        for (m in coreModules) {
            reloadModule(sender, m)
        }
    }

    private fun generateResourcePack(sender: CommandSender?) {
        val file = module!!.generateResourcePack()
        if (file != null) {
            langResourcePackGenerateSuccess!!.send(sender, file.absolutePath)
        } else {
            langResourcePackGenerateFail!!.send(sender)
        }
        if (sender is Player) {
            val dist = module!!.resourcePackDistributor
            dist.updateSha1(file!!)
            dist.sendResourcePack(sender)
        }
    }

    private fun testTomeGeneration() {
        val lootTable = LootTables.ABANDONED_MINESHAFT.lootTable
        val inventory = module!!.server.createInventory(null, 3 * 9)
        val context =
            (LootContext.Builder(
                module!!.server.worlds[0].getBlockAt(0, 0, 0).location
            )).build()
        val random = Random()

        var tomes = 0
        val simulationCount = 10000
        val gtPercentage = 0.2 // (0-2) (average 1) with 1/5 chance
        val tolerance = 0.7
        module!!.log.info("Testing ancient tome generation...")

        for (i in 0..<simulationCount) {
            inventory.clear()
            lootTable.fillInventory(inventory, random, context)
            for (`is` in inventory.storageContents) {
                if (`is` != null && `is`.hasItemMeta()) {
                    val modelData = `is`.itemMeta.customModelDataComponent.floats
                    if (!modelData.isEmpty() && modelData.first() == 0x770000.toFloat()) {
                        ++tomes
                    }
                }
            }
        }

        if (tomes == 0) {
            module!!.log.severe("0 tomes were generated in $simulationCount chests.")
        } else if (tomes > gtPercentage * simulationCount * tolerance &&
            tomes < (gtPercentage * simulationCount) / tolerance
        ) { // 70% tolerance to lower bound
            module!!
                .log.warning(
                    tomes.toString() +
                            " tomes were generated in " +
                            simulationCount +
                            " chests. This is " +
                            ((100.0 * (tomes.toDouble() / simulationCount)) / gtPercentage) +
                            "% of the expected value."
                )
        } else {
            module!!
                .log.info(
                    tomes.toString() +
                            " tomes were generated in " +
                            simulationCount +
                            " chests. This is " +
                            ((100.0 * (tomes.toDouble() / simulationCount)) / gtPercentage) +
                            "% of the expected value."
                )
        }
    }

    private fun test(sender: CommandSender?) {
        testTomeGeneration()
    }
}
