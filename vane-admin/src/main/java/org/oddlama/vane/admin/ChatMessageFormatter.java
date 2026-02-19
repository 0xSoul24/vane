package org.oddlama.vane.admin;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

public class ChatMessageFormatter extends Listener<Admin> {

    @LangMessage
    private TranslatedMessage langPlayerChatFormat;

    @LangMessage
    private TranslatedMessage langPlayerJoin;

    @LangMessage
    private TranslatedMessage langPlayerKick;

    @LangMessage
    private TranslatedMessage langPlayerQuit;

    final ChatRenderer chatRenderer;

    public ChatMessageFormatter(Context<Admin> context) {
        super(
            context.group(
                "ChatMessageFormatter",
                "Enables custom formatting of chat messages like player chats and join / quit messages."
            )
        );
        // Create custom chat renderer
        chatRenderer = new ChatRenderer() {
            public Component render(
                final Player source,
                final Component sourceDisplayName,
                final Component message,
                final Audience viewer
            ) {
                // TODO more sophisticated formatting?
                final var who = sourceDisplayName.color(NamedTextColor.AQUA);
                return langPlayerChatFormat.strComponent(who, message);
            }
        };
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        event.renderer(chatRenderer);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        event.joinMessage(null);
        langPlayerJoin.broadcastServer(event.getPlayer().playerListName().color(NamedTextColor.GOLD));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerKick(final PlayerKickEvent event) {
        // Bug in Spigot, doesn't do anything. But fixed in Paper since 1.17.
        // https://hub.spigotmc.org/jira/browse/SPIGOT-3034
        event.leaveMessage(Component.text(""));
        // message is handled in quit event
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        event.quitMessage(null);
        if (event.getReason() == PlayerQuitEvent.QuitReason.KICKED) {
            langPlayerKick.broadcastServer(event.getPlayer().playerListName().color(NamedTextColor.GOLD));
        } else {
            langPlayerQuit.broadcastServer(event.getPlayer().playerListName().color(NamedTextColor.GOLD));
        }
    }
}
