package org.oddlama.vane.portals.event;

import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.portals.portal.Portal;

public class PortalLinkConsoleEvent extends PortalEvent {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private Portal portal;
    private Block console;
    private List<Block> portalBlocks;
    private boolean checkOnly;
    private boolean cancelIfNotOwner = true;

    public PortalLinkConsoleEvent(
        final Player player,
        final Block console,
        final List<Block> portalBlocks,
        boolean checkOnly,
        @Nullable final Portal portal
    ) {
        this.player = player;
        this.console = console;
        this.portalBlocks = portalBlocks;
        this.checkOnly = checkOnly;
        this.portal = portal;
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

    public List<Block> getPortalBlocks() {
        return portalBlocks;
    }

    public boolean checkOnly() {
        return checkOnly;
    }

    public @Nullable Portal getPortal() {
        return portal;
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
        if (cancelIfNotOwner && portal != null) {
            cancelled |= !player.getUniqueId().equals(portal.owner());
        }
        return cancelled;
    }
}
