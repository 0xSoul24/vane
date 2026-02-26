package org.oddlama.vane.permissions.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collections;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.argumentType.OfflinePlayerArgumentType;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.permissions.Permissions;
import org.oddlama.vane.permissions.argumentTypes.PermissionGroupArgumentType;

@Name("permission")
@Aliases({ "perm" })
public class Permission extends Command<Permissions> {

    @LangMessage
    private TranslatedMessage langListEmpty;

    @LangMessage
    private TranslatedMessage langListHeaderGroups;

    @LangMessage
    private TranslatedMessage langListHeaderPermissions;

    @LangMessage
    private TranslatedMessage langListHeaderPlayerGroups;

    @LangMessage
    private TranslatedMessage langListHeaderPlayerPermissions;

    @LangMessage
    private TranslatedMessage langListHeaderGroupPermissions;

    @LangMessage
    private TranslatedMessage langListPlayerOffline;

    @LangMessage
    private TranslatedMessage langListGroup;

    @LangMessage
    private TranslatedMessage langListPermission;

    @LangMessage
    private TranslatedMessage langGroupAssigned;

    @LangMessage
    private TranslatedMessage langGroupRemoved;

    @LangMessage
    private TranslatedMessage langGroupAlreadyAssigned;

    @LangMessage
    private TranslatedMessage langGroupNotAssigned;

    public Permission(Context<Permissions> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        return super.getCommandBase()
            .then(help())
            .then(
                literal("list")
                    .then(
                        literal("groups")
                            .executes(ctx -> {
                                listGroups(ctx.getSource().getSender());
                                return SINGLE_SUCCESS;
                            })
                            .then(
                                argument("offlinePlayer", OfflinePlayerArgumentType.offlinePlayer()).executes(ctx -> {
                                    listGroupsForPlayer(sender(ctx), offlinePlayer(ctx));
                                    return SINGLE_SUCCESS;
                                })
                            )
                    )
                    .then(
                        literal("permissions")
                            // FIXME weirdly autocompletion works in the console
                            // but not in game ??
                            .then(
                                argument(
                                    "permissionGroup",
                                    PermissionGroupArgumentType.permissionGroup(getModule().permissionGroups)
                                ).executes(ctx -> {
                                    listPermissionsForGroup(ctx.getSource().getSender(), permissionGroup(ctx));
                                    return SINGLE_SUCCESS;
                                })
                            )
                            .then(
                                argument("offlinePlayer", OfflinePlayerArgumentType.offlinePlayer()).executes(ctx -> {
                                    listPermissionsForPlayer(ctx.getSource().getSender(), offlinePlayer(ctx));
                                    return SINGLE_SUCCESS;
                                })
                            )
                            .executes(ctx -> {
                                listPermissions(ctx.getSource().getSender());
                                return SINGLE_SUCCESS;
                            })
                    )
            )
            .then(
                literal("add").then(
                    argument("offlinePlayer", OfflinePlayerArgumentType.offlinePlayer()).then(
                        argument(
                            "permissionGroup",
                            PermissionGroupArgumentType.permissionGroup(getModule().permissionGroups)
                        ).executes(ctx -> {
                            addPlayerToGroup(
                                ctx.getSource().getSender(),
                                offlinePlayer(ctx),
                                permissionGroup(ctx)
                            );
                            return SINGLE_SUCCESS;
                        })
                    )
                )
            )
            .then(
                literal("remove").then(
                    argument("offlinePlayer", OfflinePlayerArgumentType.offlinePlayer()).then(
                        argument(
                            "permissionGroup",
                            PermissionGroupArgumentType.permissionGroup(getModule().permissionGroups)
                        ).executes(ctx -> {
                            removePlayerFromGroup(
                                ctx.getSource().getSender(),
                                offlinePlayer(ctx),
                                permissionGroup(ctx)
                            );
                            return SINGLE_SUCCESS;
                        })
                    )
                )
            );
    }

    private String permissionGroup(CommandContext<CommandSourceStack> ctx) {
        return ctx.getArgument("permissionGroup", String.class);
    }

    private Player sender(CommandContext<CommandSourceStack> ctx) {
        return (Player) ctx.getSource().getSender();
    }

    private OfflinePlayer offlinePlayer(CommandContext<CommandSourceStack> ctx) {
        return ctx.getArgument("offlinePlayer", OfflinePlayer.class);
    }

    private String permissionDefaultValueColorCode(PermissionDefault def) {
        switch (def) {
            default:
                return "§6";
            case FALSE:
                return "§c";
            case NOT_OP:
                return "§5";
            case OP:
                return "§b";
            case TRUE:
                return "§a";
        }
    }

    private String permissionValueColorCode(boolean value) {
        return permissionDefaultValueColorCode(value ? PermissionDefault.TRUE : PermissionDefault.FALSE);
    }

    private void listGroups(CommandSender sender) {
        langListHeaderGroups.send(sender);
        getModule()
            .permissionGroups.keySet()
            .stream()
            .sorted((a, b) -> a.compareTo(b))
            .forEach(group -> langListGroup.send(sender, "§b" + group));
    }

    private void listPermissions(CommandSender sender) {
        langListHeaderPermissions.send(sender);
        getModule()
            .getServer()
            .getPluginManager()
            .getPermissions()
            .stream()
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .forEach(perm ->
                langListPermission.send(
                    sender,
                    "§d" + perm.getName(),
                    permissionDefaultValueColorCode(perm.getDefault()) + perm.getDefault().toString().toLowerCase(),
                    perm.getDescription()
                )
            );
    }

    private void listPermissionsForPlayer(CommandSender sender, OfflinePlayer offlinePlayer) {
        langListHeaderPlayerPermissions.send(sender, "§b" + offlinePlayer.getName());
        var player = offlinePlayer.getPlayer();
        if (player == null) {
            // Player is offline, show configured permissions only.
            // Information from other plugins might be missing.
            langListPlayerOffline.send(sender);
            final var groups = getModule().storagePlayerGroups.get(offlinePlayer.getUniqueId());
            if (groups == null) {
                langListEmpty.send(sender);
            } else {
                for (var group : groups) {
                    listPermissionsForGroupNoHeader(sender, group);
                }
            }
        } else {
            var effectivePermissions = player.getEffectivePermissions();
            if (effectivePermissions.isEmpty()) {
                langListEmpty.send(sender);
            } else {
                player
                    .getEffectivePermissions()
                    .stream()
                    .sorted((a, b) -> a.getPermission().compareTo(b.getPermission()))
                    .forEach(att -> {
                        var perm = getModule().getServer().getPluginManager().getPermission(att.getPermission());
                        if (perm == null) {
                            getModule()
                                .getLog().warning("Encountered unregistered permission '" + att.getPermission() + "'");
                            return;
                        }
                        langListPermission.send(
                            sender,
                            "§d" + perm.getName(),
                            permissionValueColorCode(att.getValue()) + att.getValue(),
                            perm.getDescription()
                        );
                    });
            }
        }
    }

    private void listPermissionsForGroupNoHeader(CommandSender sender, String group) {
        for (var p : getModule().permissionGroups.getOrDefault(group, Collections.emptySet())) {
            var perm = getModule().getServer().getPluginManager().getPermission(p);
            if (perm == null) {
                getModule().getLog().warning("Use of unregistered permission '" + p + "' might have unintended effects.");
                langListPermission.send(sender, "§d" + p, permissionValueColorCode(true) + true, "");
            } else {
                langListPermission.send(
                    sender,
                    "§d" + perm.getName(),
                    permissionValueColorCode(true) + true,
                    perm.getDescription()
                );
            }
        }
    }

    private void listPermissionsForGroup(CommandSender sender, String group) {
        langListHeaderGroupPermissions.send(sender, "§b" + group);
        listPermissionsForGroupNoHeader(sender, group);
    }

    private void listGroupsForPlayer(CommandSender sender, OfflinePlayer offlinePlayer) {
        var set = getModule().storagePlayerGroups.get(offlinePlayer.getUniqueId());
        if (set == null) {
            langListEmpty.send(sender);
        } else {
            langListHeaderPlayerGroups.send(sender, "§b" + offlinePlayer.getName());
            for (var group : set) {
                langListGroup.send(sender, group);
            }
        }
    }

    private void addPlayerToGroup(final CommandSender sender, final OfflinePlayer player, final String group) {
        if (getModule().addPlayerToGroup(player, group)) {
            langGroupAssigned.send(sender, "§b" + player.getName(), "§a" + group);
        } else {
            langGroupAlreadyAssigned.send(sender, "§b" + player.getName(), "§a" + group);
        }
    }

    private void removePlayerFromGroup(final CommandSender sender, final OfflinePlayer player, final String group) {
        if (getModule().removePlayerFromGroup(player, group)) {
            langGroupRemoved.send(sender, "§b" + player.getName(), "§a" + group);
        } else {
            langGroupNotAssigned.send(sender, "§b" + player.getName(), "§a" + group);
        }
    }
}
