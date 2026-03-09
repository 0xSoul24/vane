package org.oddlama.vane.trifles.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.StorageUtil

@Name("setspawn")
class Setspawn(context: Context<Trifles?>) : org.oddlama.vane.core.command.Command<Trifles?>(context) {
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .requires { ctx: CommandSourceStack -> ctx.sender is Player }
            .then(help())
            .executes { ctx: CommandContext<CommandSourceStack> ->
                setSpawn(ctx.getSource().sender as Player)
                Command.SINGLE_SUCCESS
            }
    }

    private fun setSpawn(player: Player) {
        val loc = player.location

        // Unset spawn tag in all worlds
        for (world in module!!.server.worlds) {
            world.persistentDataContainer.remove(IS_SPAWN_WORLD)
        }

        // Set spawn and mark as the default world
        val world = player.world
        world.spawnLocation = loc
        world.persistentDataContainer.set(IS_SPAWN_WORLD, PersistentDataType.INTEGER, 1)
        player.sendMessage("§aSpawn §7set!")
    }

    companion object {
        @JvmField
        val IS_SPAWN_WORLD: NamespacedKey = StorageUtil.namespacedKey("vane", "is_spawn_world")
    }
}
