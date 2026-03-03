package org.oddlama.vane.permissions.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionAttachmentInfo
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.annotation.command.Aliases
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.command.argumentType.OfflinePlayerArgumentType
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.permissions.Permissions
import org.oddlama.vane.permissions.argumentTypes.PermissionGroupArgumentType
import java.util.*

@Name("permission")
@Aliases("perm")
class Permission(context: Context<Permissions?>) : org.oddlama.vane.core.command.Command<Permissions?>(context) {
    @LangMessage
    private val langListEmpty: TranslatedMessage? = null

    @LangMessage
    private val langListHeaderGroups: TranslatedMessage? = null

    @LangMessage
    private val langListHeaderPermissions: TranslatedMessage? = null

    @LangMessage
    private val langListHeaderPlayerGroups: TranslatedMessage? = null

    @LangMessage
    private val langListHeaderPlayerPermissions: TranslatedMessage? = null

    @LangMessage
    private val langListHeaderGroupPermissions: TranslatedMessage? = null

    @LangMessage
    private val langListPlayerOffline: TranslatedMessage? = null

    @LangMessage
    private val langListGroup: TranslatedMessage? = null

    @LangMessage
    private val langListPermission: TranslatedMessage? = null

    @LangMessage
    private val langGroupAssigned: TranslatedMessage? = null

    @LangMessage
    private val langGroupRemoved: TranslatedMessage? = null

    @LangMessage
    private val langGroupAlreadyAssigned: TranslatedMessage? = null

    @LangMessage
    private val langGroupNotAssigned: TranslatedMessage? = null

    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .then(help())
            .then(
                Commands.literal("list")
                    .then(
                        Commands.literal("groups")
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                listGroups(ctx.source.sender)
                                Command.SINGLE_SUCCESS
                            }
                            .then(
                                Commands.argument<OfflinePlayer>(
                                    "offlinePlayer",
                                    OfflinePlayerArgumentType.offlinePlayer()
                                ).executes { ctx: CommandContext<CommandSourceStack> ->
                                    listGroupsForPlayer(sender(ctx), offlinePlayer(ctx))
                                    Command.SINGLE_SUCCESS
                                }
                            )
                    )
                    .then(
                        Commands.literal("permissions") // FIXME weirdly autocompletion works in the console
                            // but not in game ??
                            .then(
                                Commands.argument<String>(
                                    "permissionGroup",
                                    PermissionGroupArgumentType.permissionGroup(module!!.permissionGroups)
                                ).executes { ctx: CommandContext<CommandSourceStack> ->
                                    listPermissionsForGroup(ctx.source.sender, permissionGroup(ctx))
                                    Command.SINGLE_SUCCESS
                                }
                            )
                            .then(
                                Commands.argument<OfflinePlayer>(
                                    "offlinePlayer",
                                    OfflinePlayerArgumentType.offlinePlayer()
                                ).executes { ctx: CommandContext<CommandSourceStack> ->
                                    listPermissionsForPlayer(ctx.source.sender, offlinePlayer(ctx))
                                    Command.SINGLE_SUCCESS
                                }
                            )
                            .executes { ctx: CommandContext<CommandSourceStack> ->
                                listPermissions(ctx.source.sender)
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
            .then(
                Commands.literal("add").then(
                    Commands.argument<OfflinePlayer>("offlinePlayer", OfflinePlayerArgumentType.offlinePlayer()).then(
                        Commands.argument<String>(
                            "permissionGroup",
                            PermissionGroupArgumentType.permissionGroup(module!!.permissionGroups)
                        ).executes { ctx: CommandContext<CommandSourceStack> ->
                            addPlayerToGroup(
                                ctx.source.sender,
                                offlinePlayer(ctx),
                                permissionGroup(ctx)
                            )
                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
            .then(
                Commands.literal("remove").then(
                    Commands.argument<OfflinePlayer>("offlinePlayer", OfflinePlayerArgumentType.offlinePlayer()).then(
                        Commands.argument<String>(
                            "permissionGroup",
                            PermissionGroupArgumentType.permissionGroup(module!!.permissionGroups)
                        ).executes { ctx: CommandContext<CommandSourceStack> ->
                            removePlayerFromGroup(
                                ctx.source.sender,
                                offlinePlayer(ctx),
                                permissionGroup(ctx)
                            )
                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
    }

    private fun permissionGroup(ctx: CommandContext<CommandSourceStack>): String? {
        return ctx.getArgument("permissionGroup", String::class.java)
    }

    private fun sender(ctx: CommandContext<CommandSourceStack>): Player {
        return ctx.source.sender as Player
    }

    private fun offlinePlayer(ctx: CommandContext<CommandSourceStack>): OfflinePlayer {
        return ctx.getArgument("offlinePlayer", OfflinePlayer::class.java)!!
    }

    private fun permissionDefaultValueColorCode(def: PermissionDefault): String {
        return when (def) {
            PermissionDefault.FALSE -> "§c"
            PermissionDefault.NOT_OP -> "§5"
            PermissionDefault.OP -> "§b"
            PermissionDefault.TRUE -> "§a"
        }
    }

    private fun permissionValueColorCode(value: Boolean): String {
        return permissionDefaultValueColorCode(if (value) PermissionDefault.TRUE else PermissionDefault.FALSE)
    }

    private fun listGroups(sender: CommandSender?) {
        langListHeaderGroups!!.send(sender)
        module!!.permissionGroups.keys.stream().sorted { a: String, b: String -> a.compareTo(b) }
            .forEach { group: String -> langListGroup!!.send(sender, "§b$group") }
    }

    private fun listPermissions(sender: CommandSender?) {
        langListHeaderPermissions!!.send(sender)
        module!!.server.pluginManager.permissions.stream().sorted { a: Permission, b: Permission -> a.name.compareTo(b.name) }
            .forEach { perm: Permission ->
                langListPermission!!.send(
                    sender,
                    "§d" + perm.name,
                    permissionDefaultValueColorCode(perm.default) + perm.default.toString()
                        .lowercase(Locale.getDefault()),
                    perm.description
                )
            }
    }

    private fun listPermissionsForPlayer(sender: CommandSender?, offlinePlayer: OfflinePlayer) {
        langListHeaderPlayerPermissions!!.send(sender, "§b" + offlinePlayer.name)
        val player = offlinePlayer.player
        if (player == null) {
            // Player is offline, show configured permissions only.
            // Information from other plugins might be missing.
            langListPlayerOffline!!.send(sender)
            val groups: MutableSet<String>? = module!!.storagePlayerGroups[offlinePlayer.uniqueId]
            if (groups == null) {
                langListEmpty!!.send(sender)
            } else {
                for (group in groups) {
                    listPermissionsForGroupNoHeader(sender, group)
                }
            }
        } else {
            val effectivePermissions = player.effectivePermissions
            if (effectivePermissions.isEmpty()) {
                langListEmpty!!.send(sender)
            } else {
                player.effectivePermissions.stream()
                    .sorted { a: PermissionAttachmentInfo, b: PermissionAttachmentInfo -> a.permission.compareTo(b.permission) }
                    .forEach { att: PermissionAttachmentInfo ->
                        val perm: Permission? = module!!.server.pluginManager.getPermission(att.permission)
                        if (perm == null) {
                            module!!.log.warning("Encountered unregistered permission '" + att.permission + "'")
                            return@forEach
                        }
                        langListPermission!!.send(
                            sender,
                            "§d" + perm.name,
                            permissionValueColorCode(att.value) + att.value,
                            perm.description
                        )
                    }
            }
        }
    }

    private fun listPermissionsForGroupNoHeader(sender: CommandSender?, group: String?) {
        for (p in module!!.permissionGroups.getOrDefault(group, mutableSetOf())) {
            val perm: Permission? = module!!.server.pluginManager.getPermission(p)
            if (perm == null) {
                module!!.log.warning("Use of unregistered permission '$p' might have unintended effects.")
                langListPermission!!.send(sender, "§d$p", permissionValueColorCode(true) + true, "")
            } else {
                langListPermission!!.send(
                    sender,
                    "§d" + perm.name,
                    permissionValueColorCode(true) + true,
                    perm.description
                )
            }
        }
    }

    private fun listPermissionsForGroup(sender: CommandSender?, group: String?) {
        langListHeaderGroupPermissions!!.send(sender, "§b$group")
        listPermissionsForGroupNoHeader(sender, group)
    }

    private fun listGroupsForPlayer(sender: CommandSender?, offlinePlayer: OfflinePlayer) {
        val set: MutableSet<String>? = module!!.storagePlayerGroups[offlinePlayer.uniqueId]
        if (set == null) {
            langListEmpty!!.send(sender)
        } else {
            langListHeaderPlayerGroups!!.send(sender, "§b" + offlinePlayer.name)
            for (group in set) {
                langListGroup!!.send(sender, group)
            }
        }
    }

    private fun addPlayerToGroup(sender: CommandSender?, player: OfflinePlayer, group: String?) {
        if (module!!.addPlayerToGroup(player, group)) {
            langGroupAssigned!!.send(sender, "§b" + player.name, "§a$group")
        } else {
            langGroupAlreadyAssigned!!.send(sender, "§b" + player.name, "§a$group")
        }
    }

    private fun removePlayerFromGroup(sender: CommandSender?, player: OfflinePlayer, group: String?) {
        if (module!!.removePlayerFromGroup(player, group)) {
            langGroupRemoved!!.send(sender, "§b" + player.name, "§a$group")
        } else {
            langGroupNotAssigned!!.send(sender, "§b" + player.name, "§a$group")
        }
    }
}
