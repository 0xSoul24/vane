package org.oddlama.vane.core.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
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
import java.util.Random

@Name("vane")
class Vane(context: Context<Core?>) : org.oddlama.vane.core.command.Command<Core?>(context) {
    @LangMessage private val langReloadSuccess: TranslatedMessage? = null
    @LangMessage private val langReloadFail: TranslatedMessage? = null
    @LangMessage private val langResourcePackGenerateSuccess: TranslatedMessage? = null
    @LangMessage private val langResourcePackGenerateFail: TranslatedMessage? = null

    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> =
        super.getCommandBase()
            .then(help())
            .then(
                Commands.literal("reload")
                    .executes { ctx ->
                        reloadAll(ctx.source.sender)
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        Commands.argument<Module<*>>("module", ModuleArgumentType.module(module!!))
                            .executes { ctx ->
                                reloadModule(
                                    ctx.source.sender,
                                    ctx.getArgument("module", Module::class.java) as Module<*>
                                )
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
            .then(
                Commands.literal("generate_resource_pack")
                    .executes { ctx ->
                        generateResourcePack(ctx.source.sender)
                        Command.SINGLE_SUCCESS
                    }
            )
            .then(
                Commands.literal("test_do_not_use_if_you_are_not_a_dev")
                    .executes { ctx ->
                        test(ctx.source.sender)
                        Command.SINGLE_SUCCESS
                    }
            )

    private fun reloadModule(sender: CommandSender?, module: Module<*>) {
        val msg = if (module.reloadConfiguration()) langReloadSuccess else langReloadFail
        msg!!.send(sender, "§bvane-${module.annotationName}")
    }

    private fun reloadAll(sender: CommandSender?) {
        module!!.core?.modules?.filterNotNull()?.forEach { reloadModule(sender, it) }
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
        val context = LootContext.Builder(
            module!!.server.worlds[0].getBlockAt(0, 0, 0).location
        ).build()
        val random = Random()

        val simulationCount = 10000
        val gtPercentage = 0.2
        val tolerance = 0.7
        var tomes = 0

        module!!.log.info("Testing ancient tome generation...")

        repeat(simulationCount) {
            inventory.clear()
            lootTable.fillInventory(inventory, random, context)
            inventory.storageContents.forEach { item ->
                if (item != null && item.hasItemMeta()) {
                    val modelData = item.itemMeta.customModelDataComponent.floats
                    if (modelData.isNotEmpty() && modelData.first() == 0x770000.toFloat()) tomes++
                }
            }
        }

        val percentage = (100.0 * tomes.toDouble() / simulationCount) / gtPercentage
        val msg = "$tomes tomes were generated in $simulationCount chests. This is $percentage% of the expected value."

        when {
            tomes == 0 -> module!!.log.severe("0 tomes were generated in $simulationCount chests.")
            tomes > gtPercentage * simulationCount * tolerance &&
            tomes < (gtPercentage * simulationCount) / tolerance ->
                module!!.log.warning(msg)
            else -> module!!.log.info(msg)
        }
    }

    private fun test(sender: CommandSender?) = testTomeGeneration()
}
