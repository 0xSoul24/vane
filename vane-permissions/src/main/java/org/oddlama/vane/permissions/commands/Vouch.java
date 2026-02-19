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
    private TranslatedMessage langVouched;

    @LangMessage
    private TranslatedMessage langAlreadyVouched;

    @ConfigString(def = "user", desc = "The group to assign to players when someone vouches for them.", metrics = true)
    private String configVouchGroup;

    // Persistent storage
    @Persistent
    public Map<UUID, Set<UUID>> storageVouchedBy = new HashMap<>();

    public Vouch(Context<Permissions> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        return super.getCommandBase()
            .then(help())
            .then(
                argument("offline_player", OfflinePlayerArgumentType.offlinePlayer()).executes(ctx -> {
                    vouchForPlayer(
                        (Player) ctx.getSource().getSender(),
                        ctx.getArgument("offline_player", OfflinePlayer.class)
                    );
                    return SINGLE_SUCCESS;
                })
            );
    }

    private void vouchForPlayer(final Player sender, final OfflinePlayer vouchedPlayer) {
        var vouchedBySet = storageVouchedBy.computeIfAbsent(vouchedPlayer.getUniqueId(), k -> new HashSet<>());

        if (vouchedBySet.add(sender.getUniqueId())) {
            // If it was the first one, we assign the group,
            // otherwise we just record that the player also vouched.
            if (vouchedBySet.size() == 1) {
                getModule().addPlayerToGroup(vouchedPlayer, configVouchGroup);
            }

            langVouched.send(sender, "§b" + vouchedPlayer.getName());
        } else {
            langAlreadyVouched.send(sender, "§b" + vouchedPlayer.getName());
        }

        markPersistentStorageDirty();
    }
}
