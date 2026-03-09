package org.oddlama.vane.trifles.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigMaterial
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.functional.Function4
import org.oddlama.vane.core.material.HeadMaterial
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.menu.MenuFactory
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.PlayerUtil
import java.util.function.Predicate

@Name("heads")
class Heads(context: Context<Trifles?>) :
    org.oddlama.vane.core.command.Command<Trifles?>(context, PermissionDefault.TRUE) {
    @ConfigMaterial(def = Material.BONE, desc = "Currency material used to buy heads.")
    var configCurrency: Material? = null

    @ConfigInt(def = 1, min = 0, desc = "Price (in currency) per head. Set to 0 for free heads.")
    var configPricePerHead: Int = 0

    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .requires { ctx: CommandSourceStack -> ctx.sender is Player }
            .then(help())
            .executes { ctx: CommandContext<CommandSourceStack> ->
                openHeadLibrary(ctx.getSource().sender as Player)
                Command.SINGLE_SUCCESS
            }
    }

    private fun openHeadLibrary(player: Player) {
        MenuFactory.headSelector(
            getContext()!!,
            player,
            { player2: Player?, m: Menu?, t: HeadMaterial?, event: InventoryClickEvent? ->
                val amount: Int = when (event!!.click) {
                    ClickType.NUMBER_KEY -> event.hotbarButton + 1
                    ClickType.LEFT -> 1
                    ClickType.RIGHT -> 32
                    ClickType.MIDDLE, ClickType.SHIFT_LEFT -> 64
                    ClickType.SHIFT_RIGHT -> 16
                    else -> return@headSelector Menu.ClickResult.INVALID_CLICK
                }

                // Take currency items
                if (configPricePerHead > 0 &&
                    !PlayerUtil.takeItems(player2!!, ItemStack(configCurrency!!, configPricePerHead * amount))
                ) {
                    return@headSelector Menu.ClickResult.ERROR
                }

                PlayerUtil.giveItems(player2!!, t!!.item(), amount)
                Menu.ClickResult.SUCCESS
            },
            { player2: Player? -> }
        ).open(player)
    }
}
