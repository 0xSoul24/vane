package org.oddlama.vane.permissions

import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.RemoteServerCommandEvent
import org.bukkit.event.server.ServerCommandEvent
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.annotation.config.ConfigStringListMap
import org.oddlama.vane.annotation.config.ConfigStringListMapEntry
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.permissions.commands.Permission
import org.oddlama.vane.permissions.commands.Vouch
import java.util.*

@VaneModule(name = "permissions", bstats = 8641, configVersion = 1, langVersion = 1, storageVersion = 1)
class Permissions : Module<Permissions?>() {
    // Configuration
    @ConfigBoolean(
        def = false,
        desc = "Remove all default permissions from ANY SOURCE (including other plugins and minecraft permissions) to start with a clean preset. This will allow you to exactly set which player have which permissions instead of having to resort to volatile stateful changes like negative permissions. This will result in OPed players to lose access to commands, if not explicitly added back via permissions. The wildcard permissions can be viewed using `perms list permissions`. The wildcard permissions `minecraft` and `craftbukkit` may be especially useful."
    )
    var configRemoveDefaults: Boolean = false

    @ConfigString(
        def = "default",
        desc = "The permission group that will be given to players that have no other permission group."
    )
    var configDefaultGroup: String? = null

    @ConfigStringListMap(
        def = [ConfigStringListMapEntry(
            key = "default",
            list = ["bukkit.command.help", "bukkit.broadcast", "bukkit.broadcast.user"]
        ), ConfigStringListMapEntry(
            key = "user",
            list = ["vane.permissions.groups.default", "vane.admin.modify_world", "vane.regions.commands.region", "vane.trifles.commands.heads"
            ]
        ), ConfigStringListMapEntry(
            key = "verified",
            list = ["vane.permissions.groups.user", "vane.permissions.commands.vouch"]
        ), ConfigStringListMapEntry(
            key = "admin",
            list = ["vane.permissions.groups.verified", "vane.admin.bypass_spawn_protection", "vane.portals.admin", "vane.regions.admin", "vane.*.commands.*"
            ]
        )],
        desc = "The permission groups. A player can have multiple permission groups assigned. Permission groups can inherit other permission groups by specifying vane.permissions.groups.<groupname> as a permission."
    )
    var configGroups: MutableMap<String, MutableList<String>>? = null

    // Persistent storage
    @Persistent
    var storagePlayerGroups: MutableMap<UUID, MutableSet<String>> = HashMap<UUID, MutableSet<String>>()

    // Variables
    val permissionGroups: MutableMap<String, MutableSet<String>> = HashMap<String, MutableSet<String>>()
    private val playerAttachments: MutableMap<UUID, PermissionAttachment> = HashMap<UUID, PermissionAttachment>()

    override fun onModuleEnable() {
        scheduleNextTick {
            if (configRemoveDefaults) {
                for (perm in module!!.server.pluginManager.permissions) {
                    perm.default = PermissionDefault.FALSE
                    module!!.server.pluginManager.recalculatePermissionDefaults(perm)

                    // But still allow the console to execute commands
                    this@Permissions.addConsolePermission(perm)
                }
            }
        }
    }

    override fun onConfigChange() {
        flattenGroups()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        registerPlayer(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerKick(event: PlayerKickEvent) {
        unregisterPlayer(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        unregisterPlayer(event.player)
    }

    private val senderAttachments: MutableMap<CommandSender?, PermissionAttachment?> =
        HashMap<CommandSender?, PermissionAttachment?>()

    init {
        Permission(this)
        Vouch(this)
    }

    private fun addConsolePermissions(sender: CommandSender) {
        // Register attachment for sender if not done already
        if (!senderAttachments.containsKey(sender)) {
            val attachment = sender.addAttachment(this)
            senderAttachments[sender] = attachment

            val attachedPerms: MutableMap<String, Boolean> = module!!.consoleAttachment?.permissions ?: mutableMapOf()
            attachedPerms.forEach { (p: String, v: Boolean) -> attachment.setPermission(p, v) }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onServerCommandEvent(event: ServerCommandEvent) {
        val sender = event.sender
        if (sender is Player && sender.isOp) {
            // Console command sender will always have the correct permission attachment
            // Command block shall be ignored for now (causes lag, see #178)
            addConsolePermissions(sender)
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onRemoteServerCommandEvent(event: RemoteServerCommandEvent) {
        val sender = event.sender
        if (sender.isOp) {
            addConsolePermissions(sender)
        }
    }

    /** Resolve references to other permission groups in the hierarchy.  */
    private fun flattenGroups() {
        permissionGroups.clear()
        configGroups?.forEach { (k: String, v: MutableList<String>) ->
            val set = HashSet<String>()
            for (perm in v) {
                if (!perm.startsWith("vane.permissions.groups.")) {
                    set.add(perm)
                }
            }
            permissionGroups[k] = set
        }

        // Resolve group inheritance
        var modified = true
        while (modified) {
            modified = false
            configGroups?.forEach { (k: String, v: MutableList<String>) ->
                val set: MutableSet<String> = permissionGroups[k] ?: return@forEach
                permLoop@ for (perm in v) {
                    if (perm.startsWith("vane.permissions.groups.")) {
                        val group = perm.substring("vane.permissions.groups.".length)
                        val groupPerms = permissionGroups[group]
                        if (groupPerms == null) {
                            log.severe(
                                "Nonexistent permission group '" +
                                        group +
                                        "' referenced by group '" +
                                        k +
                                        "'; Ignoring statement!"
                            )
                            continue@permLoop
                        }
                        if (set.addAll(groupPerms)) modified = true
                    }
                }
            }
        }
    }

    private fun registerPlayer(player: Player) {
        // Register PermissionAttachment
        val attachment = player.addAttachment(this)
        playerAttachments[player.uniqueId] = attachment

        // Attach permissions
        recalculatePlayerPermissions(player)
    }

    fun recalculatePlayerPermissions(player: Player) {
        // Clear attachment
        val attachment: PermissionAttachment = playerAttachments[player.uniqueId]!!
        val attachedPerms = attachment.permissions
        attachedPerms.forEach { (p: String, _) -> attachment.unsetPermission(p) }

        // Add permissions again
        var groups = storagePlayerGroups[player.uniqueId]
        if (groups.isNullOrEmpty()) {
            // Assign player to a default permission group
            val defaultGroup = configDefaultGroup ?: "default"
            groups = mutableSetOf(defaultGroup)
        }

        for (group in groups) {
            for (p in permissionGroups.getOrDefault(group, mutableSetOf())) {
                val perm = module!!.server.pluginManager.getPermission(p)
                if (perm == null) {
                    log.warning("Use of unregistered permission '$p' might have unintended effects.")
                }
                attachment.setPermission(p, true)
            }
        }

        // Update list of commands for client side root tab completion
        player.updateCommands()
    }

    private fun unregisterPlayer(player: Player) {
        val attachment = playerAttachments.remove(player.uniqueId)
        if (attachment != null) {
            player.removeAttachment(attachment)
        }
    }

    fun saveAndRecalculate(player: OfflinePlayer) {
        markPersistentStorageDirty()

        // Recalculate permissions if player is currently online
        if (player.isOnline) {
            recalculatePlayerPermissions(player.player!!)
        }
    }

    fun addPlayerToGroup(player: OfflinePlayer, group: String?): Boolean {
        val set = storagePlayerGroups.computeIfAbsent(player.uniqueId) { HashSet<String>() }

        val added = set.add(group ?: return false)
        if (added) {
            log.info("[audit] Group " + group + " assigned to " + player.uniqueId + " (" + player.name + ")")
            saveAndRecalculate(player)
        }

        return added
    }

    fun removePlayerFromGroup(player: OfflinePlayer, group: String?): Boolean {
        val set = storagePlayerGroups[player.uniqueId]
        var removed = false
        if (set != null) {
            removed = set.remove(group)
        }

        if (removed) {
            log.info(
                "[audit] Group " + group + " removed from " + player.uniqueId + " (" + player.name + ")"
            )
            saveAndRecalculate(player)
        }

        return removed
    }
}
