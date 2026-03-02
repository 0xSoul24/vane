package org.oddlama.vane.admin

import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context

class ChatMessageFormatter(context: Context<Admin?>) : Listener<Admin?>(
    context.group(
        "ChatMessageFormatter",
        "Enables custom formatting of chat messages like player chats and join / quit messages."
    )
) {
    @LangMessage
    private val langPlayerChatFormat: TranslatedMessage? = null

    @LangMessage
    private val langPlayerJoin: TranslatedMessage? = null

    @LangMessage
    private val langPlayerKick: TranslatedMessage? = null

    @LangMessage
    private val langPlayerQuit: TranslatedMessage? = null

    // Create custom chat renderer
    val chatRenderer: ChatRenderer = ChatRenderer { source, sourceDisplayName, message, viewer -> // TODO more sophisticated formatting?
        val who = sourceDisplayName.color(NamedTextColor.AQUA)
        langPlayerChatFormat!!.strComponent(who, message)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerChat(event: AsyncChatEvent) {
        event.renderer(chatRenderer)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.joinMessage(null)
        langPlayerJoin!!.broadcastServer(event.getPlayer().playerListName().color(NamedTextColor.GOLD))
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerKick(event: PlayerKickEvent) {
        // Bug in Spigot, doesn't do anything. But fixed in Paper since 1.17.
        // https://hub.spigotmc.org/jira/browse/SPIGOT-3034
        event.leaveMessage(Component.text(""))
        // message is handled in quit event
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        event.quitMessage(null)
        if (event.reason == PlayerQuitEvent.QuitReason.KICKED) {
            langPlayerKick!!.broadcastServer(event.getPlayer().playerListName().color(NamedTextColor.GOLD))
        } else {
            langPlayerQuit!!.broadcastServer(event.getPlayer().playerListName().color(NamedTextColor.GOLD))
        }
    }
}
