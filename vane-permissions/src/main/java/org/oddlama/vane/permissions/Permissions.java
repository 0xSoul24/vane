package org.oddlama.vane.permissions;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.config.ConfigStringListMap;
import org.oddlama.vane.annotation.config.ConfigStringListMapEntry;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.module.Module;

@VaneModule(name = "permissions", bstats = 8641, configVersion = 1, langVersion = 1, storageVersion = 1)
public class Permissions extends Module<Permissions> {

    // Configuration
    @ConfigBoolean(
        def = false,
        desc = "Remove all default permissions from ANY SOURCE (including other plugins and minecraft permissions) to start with a clean preset. This will allow you to exactly set which player have which permissions instead of having to resort to volatile stateful changes like negative permissions. This will result in OPed players to lose access to commands, if not explicitly added back via permissions. The wildcard permissions can be viewed using `perms list permissions`. The wildcard permissions `minecraft` and `craftbukkit` may be especially useful."
    )
    public boolean configRemoveDefaults;

    @ConfigString(
        def = "default",
        desc = "The permission group that will be given to players that have no other permission group."
    )
    public String configDefaultGroup;

    @ConfigStringListMap(
        def = {
            @ConfigStringListMapEntry(
                key = "default",
                list = { "bukkit.command.help", "bukkit.broadcast", "bukkit.broadcast.user" }
            ),
            @ConfigStringListMapEntry(
                key = "user",
                list = {
                    "vane.permissions.groups.default",
                    "vane.admin.modify_world",
                    "vane.regions.commands.region",
                    "vane.trifles.commands.heads",
                }
            ),
            @ConfigStringListMapEntry(
                key = "verified",
                list = { "vane.permissions.groups.user", "vane.permissions.commands.vouch" }
            ),
            @ConfigStringListMapEntry(
                key = "admin",
                list = {
                    "vane.permissions.groups.verified",
                    "vane.admin.bypass_spawn_protection",
                    "vane.portals.admin",
                    "vane.regions.admin",
                    "vane.*.commands.*",
                }
            ),
        },
        desc = "The permission groups. A player can have multiple permission groups assigned. Permission groups can inherit other permission groups by specifying vane.permissions.groups.<groupname> as a permission."
    )
    public Map<String, List<String>> configGroups;

    // Persistent storage
    @Persistent
    public Map<UUID, Set<String>> storagePlayerGroups = new HashMap<>();

    // Variables
    public final Map<String, Set<String>> permissionGroups = new HashMap<>();
    private final Map<UUID, PermissionAttachment> playerAttachments = new HashMap<>();

    public Permissions() {
        new org.oddlama.vane.permissions.commands.Permission(this);
        new org.oddlama.vane.permissions.commands.Vouch(this);
    }

    @Override
    public void onModuleEnable() {
        scheduleNextTick(() -> {
            if (configRemoveDefaults) {
                for (var perm : getServer().getPluginManager().getPermissions()) {
                    perm.setDefault(PermissionDefault.FALSE);
                    getServer().getPluginManager().recalculatePermissionDefaults(perm);

                    // But still allow the console to execute commands
                    Permissions.this.addConsolePermission(perm);
                }
            }
        });
    }

    @Override
    public void onConfigChange() {
        flattenGroups();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        registerPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        unregisterPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        unregisterPlayer(event.getPlayer());
    }

    private final Map<CommandSender, PermissionAttachment> senderAttachments = new HashMap<>();

    private void addConsolePermissions(final CommandSender sender) {
        // Register attachment for sender if not done already
        if (!senderAttachments.containsKey(sender)) {
            final var attachment = sender.addAttachment(this);
            senderAttachments.put(sender, attachment);

            final var attachedPerms = getConsoleAttachment().getPermissions();
            attachedPerms.forEach((p, v) -> attachment.setPermission(p, v));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onServerCommandEvent(ServerCommandEvent event) {
        final var sender = event.getSender();
        if (sender instanceof Player && sender.isOp()) {
            // Console command sender will always have the correct permission attachment
            // Command block shall be ignored for now (causes lag, see #178)
            addConsolePermissions(sender);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRemoteServerCommandEvent(RemoteServerCommandEvent event) {
        final var sender = event.getSender();
        if (sender.isOp()) {
            addConsolePermissions(sender);
        }
    }

    /** Resolve references to other permission groups in the hierarchy. */
    private void flattenGroups() {
        permissionGroups.clear();
        configGroups.forEach((k, v) -> {
            final var set = new HashSet<String>();
            for (var perm : v) {
                if (perm.startsWith("vane.permissions.groups.")) {} else {
                    set.add(perm);
                }
            }
            permissionGroups.put(k, set);
        });

        // Resolve group inheritance
        var modified = new Object() {
            boolean value = false;
        };
        do {
            modified.value = false;
            configGroups.forEach((k, v) -> {
                final var set = permissionGroups.get(k);
                for (var perm : v) {
                    if (perm.startsWith("vane.permissions.groups.")) {
                        final var group = perm.substring("vane.permissions.groups.".length());
                        final var groupPerms = permissionGroups.get(group);
                        if (groupPerms == null) {
                            getLog().severe(
                                "Nonexistent permission group '" +
                                group +
                                "' referenced by group '" +
                                k +
                                "'; Ignoring statement!"
                            );
                            continue;
                        }
                        modified.value |= set.addAll(groupPerms);
                    }
                }
            });
        } while (modified.value);
    }

    private void registerPlayer(final Player player) {
        // Register PermissionAttachment
        final var attachment = player.addAttachment(this);
        playerAttachments.put(player.getUniqueId(), attachment);

        // Attach permissions
        recalculatePlayerPermissions(player);
    }

    public void recalculatePlayerPermissions(final Player player) {
        // Clear attachment
        final var attachment = playerAttachments.get(player.getUniqueId());
        final var attachedPerms = attachment.getPermissions();
        attachedPerms.forEach((p, v) -> attachment.unsetPermission(p));

        // Add permissions again
        var groups = storagePlayerGroups.get(player.getUniqueId());
        if (groups == null || groups.isEmpty()) {
            // Assign player to a default permission group
            groups = Set.of(configDefaultGroup);
        }

        for (var group : groups) {
            for (var p : permissionGroups.getOrDefault(group, Collections.emptySet())) {
                final var perm = getServer().getPluginManager().getPermission(p);
                if (perm == null) {
                    getLog().warning("Use of unregistered permission '" + p + "' might have unintended effects.");
                }
                attachment.setPermission(p, true);
            }
        }

        // Update list of commands for client side root tab completion
        player.updateCommands();
    }

    private void unregisterPlayer(final Player player) {
        final var attachment = playerAttachments.remove(player.getUniqueId());
        if (attachment != null) {
            player.removeAttachment(attachment);
        }
    }

    public void saveAndRecalculate(final OfflinePlayer player) {
        markPersistentStorageDirty();

        // Recalculate permissions if player is currently online
        if (player.isOnline()) {
            recalculatePlayerPermissions(player.getPlayer());
        }
    }

    public boolean addPlayerToGroup(final OfflinePlayer player, final String group) {
        var set = storagePlayerGroups.computeIfAbsent(player.getUniqueId(), k -> new HashSet<String>());

        final var added = set.add(group);
        if (added) {
            getLog().info("[audit] Group " + group + " assigned to " + player.getUniqueId() + " (" + player.getName() + ")");
            saveAndRecalculate(player);
        }

        return added;
    }

    public boolean removePlayerFromGroup(final OfflinePlayer player, final String group) {
        var set = storagePlayerGroups.get(player.getUniqueId());
        var removed = false;
        if (set != null) {
            removed = set.remove(group);
        }

        if (removed) {
            getLog().info(
                "[audit] Group " + group + " removed from " + player.getUniqueId() + " (" + player.getName() + ")"
            );
            saveAndRecalculate(player);
        }

        return removed;
    }
}
