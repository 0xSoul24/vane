package org.oddlama.vane.trifles.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.StorageUtil;

@Name("setspawn")
public class Setspawn extends Command<Trifles> {

    public static final NamespacedKey IS_SPAWN_WORLD = StorageUtil.namespacedKey("vane", "is_spawn_world");

    public Setspawn(Context<Trifles> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        return super.getCommandBase()
            .requires(ctx -> ctx.getSender() instanceof Player)
            .then(help())
            .executes(ctx -> {
                setSpawn((Player) ctx.getSource().getSender());
                return SINGLE_SUCCESS;
            });
    }

    private void setSpawn(Player player) {
        final var loc = player.getLocation();

        // Unset spawn tag in all worlds
        for (final var world : getModule().getServer().getWorlds()) {
            world.getPersistentDataContainer().remove(IS_SPAWN_WORLD);
        }

        // Set spawn and mark as the default world
        final var world = player.getWorld();
        world.setSpawnLocation(loc);
        world.getPersistentDataContainer().set(IS_SPAWN_WORLD, PersistentDataType.INTEGER, 1);
        player.sendMessage("§aSpawn §7set!");
    }
}
