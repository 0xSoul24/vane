package org.oddlama.vane.portals.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.oddlama.vane.portals.portal.Portal;

public class PortalUnlinkConsoleEvent extends PortalEvent {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private Block console;
    private Portal portal;
    private boolean checkOnly;
    private boolean cancelIfNotOwner = true;

    public PortalUnlinkConsoleEvent(final Player player, final Block console, final Portal portal, boolean checkOnly) {
        this.player = player;
        this.console = console;
        this.portal = portal;
        this.checkOnly = checkOnly;
    }

    public void setCancelIfNotOwner(boolean cancelIfNotOwner) {
        this.cancelIfNotOwner = cancelIfNotOwner;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getConsole() {
        return console;
    }

    public Portal getPortal() {
        return portal;
    }

    public boolean checkOnly() {
        return checkOnly;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        var cancelled = super.isCancelled();
        if (cancelIfNotOwner) {
            cancelled |= !player.getUniqueId().equals(portal.owner());
        }
        return cancelled;
    }
}
