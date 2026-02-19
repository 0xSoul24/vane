package org.oddlama.vane.portals.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.oddlama.vane.portals.portal.PortalBoundary;

public class PortalConstructEvent extends PortalEvent {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private PortalBoundary boundary;
    private boolean checkOnly;

    public PortalConstructEvent(final Player player, final PortalBoundary boundary, boolean checkOnly) {
        this.player = player;
        this.boundary = boundary;
        this.checkOnly = checkOnly;
    }

    public Player getPlayer() {
        return player;
    }

    public PortalBoundary getBoundary() {
        return boundary;
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
}
