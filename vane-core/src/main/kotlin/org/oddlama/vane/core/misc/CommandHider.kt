package org.oddlama.vane.core.misc

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context

class CommandHider(context: Context<Core?>) : Listener<Core?>(
    context.group(
        "HideCommands",
        "Hide error messages for all commands for which a player has no permission, by displaying the default unknown command message instead."
    )
) {
    private fun allowCommandEvent(message: String, player: Player): Boolean {
        var message = message
        message = message.trim { it <= ' ' }
        if (!message.startsWith("/")) {
            return false
        }

        var id = message.substring(1)
        val spaceIndex = id.indexOf(' ')
        if (spaceIndex > -1) {
            id = id.substring(0, spaceIndex)
        }

        val commandMap = module!!.server.commandMap.knownCommands
        val command = commandMap[id]
        if (command != null) {
            return command.testPermissionSilent(player)
        }

        return true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        if (!allowCommandEvent(event.message, event.getPlayer())) {
            // Use a hardcoded default message instead of deprecated getSpigotConfig()
            val msg = "Unknown command. Type \"/help\" for help."
            event.getPlayer().sendMessage(msg)
            event.isCancelled = true
        }
    }
}
