package org.oddlama.vane.permissions.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.argumentType.OfflinePlayerArgumentType;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.permissions.Permissions;

@Name("vouch")
public class Vouch extends Command<Permissions> {

    @LangMessage
    private TranslatedMessage lang_vouched;

    @LangMessage
    private TranslatedMessage lang_already_vouched;

    @ConfigString(def = "user", desc = "The group to assign to players when someone vouches for them.", metrics = true)
    private String config_vouch_group;

    // Persistent storage
    @Persistent
    public Map<UUID, Set<UUID>> storage_vouched_by = new HashMap<>();

    public Vouch(Context<Permissions> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
        return super.get_command_base()
                .then(help())
                .then(
                        argument("offline_player", OfflinePlayerArgumentType.offlinePlayer()).executes(ctx -> {
                            vouch_for_player(
                                    (Player) ctx.getSource().getSender(),
                                    ctx.getArgument("offline_player", OfflinePlayer.class)
                            );
                            return SINGLE_SUCCESS;
                        })
                );
    }

    private void vouch_for_player(final Player sender, final OfflinePlayer vouched_player) {
        var vouched_by_set = storage_vouched_by.computeIfAbsent(vouched_player.getUniqueId(), k -> new HashSet<>());

        if (vouched_by_set.add(sender.getUniqueId())) {
            // If it was the first one, we assign the group,
            // otherwise we just record that the player also vouched.
            if (vouched_by_set.size() == 1) {
                get_module().add_player_to_group(vouched_player, config_vouch_group);
            }

            lang_vouched.send(sender, "§b" + vouched_player.getName());
        } else {
            lang_already_vouched.send(sender, "§b" + vouched_player.getName());
        }

        mark_persistent_storage_dirty();
    }
}
