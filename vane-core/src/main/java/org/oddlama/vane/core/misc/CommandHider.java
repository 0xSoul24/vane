package org.oddlama.vane.core.misc;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class CommandHider extends Listener<Core> {

    public CommandHider(Context<Core> context) {
        super(
            context.group(
                "HideCommands",
                "Hide error messages for all commands for which a player has no permission, by displaying the default unknown command message instead."
            )
        );
    }

    private boolean allowCommandEvent(String message, Player player) {
        message = message.trim();
        if (!message.startsWith("/")) {
            return false;
        }

        var id = message.substring(1);
        final var spaceIndex = id.indexOf(' ');
        if (spaceIndex > -1) {
            id = id.substring(0, spaceIndex);
        }

        final var commandMap = getModule().getServer().getCommandMap().getKnownCommands();
        var command = commandMap.get(id);
        if (command != null) {
            return command.testPermissionSilent(player);
        }

        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!allowCommandEvent(event.getMessage(), event.getPlayer())) {
            // Use a hardcoded default message instead of deprecated getSpigotConfig()
            final var msg = "Unknown command. Type \"/help\" for help.";
            event.getPlayer().sendMessage(msg);
            event.setCancelled(true);
        }
    }
}
