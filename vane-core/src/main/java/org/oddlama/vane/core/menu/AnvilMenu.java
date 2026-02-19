package org.oddlama.vane.core.menu;

import static org.oddlama.vane.util.Nms.playerHandle;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.oddlama.vane.core.module.Context;

public class AnvilMenu extends Menu {

    private ServerPlayer entity;
    private AnvilContainer container;
    private int containerId;
    private String title;

    public AnvilMenu(final Context<?> context, final org.bukkit.entity.Player player, final String title) {
        super(context);
        this.title = title;
        this.entity = playerHandle(player);
        this.containerId = entity.nextContainerCounter();
        this.container = new AnvilContainer(containerId, entity);
        this.container.setTitle(Component.literal(title));
        this.inventory = container.getBukkitView().getTopInventory();
    }

    @Override
    public void openWindow(final org.bukkit.entity.Player player) {
        if (tainted) {
            return;
        }

        if (playerHandle(player) != entity) {
            manager
                .getModule()
                .log.warning("AnvilMenu.open() was called with a player for whom this inventory wasn't created!");
        }

        entity.connection.send(
            new ClientboundOpenScreenPacket(containerId, container.getType(), Component.literal(title))
        );
        entity.initMenu(container);
        entity.containerMenu = container;
    }

    private class AnvilContainer extends net.minecraft.world.inventory.AnvilMenu {

        public AnvilContainer(int windowId, final Player entity) {
            super(windowId, entity.getInventory(), ContainerLevelAccess.create(entity.level(), new BlockPos(0, 0, 0)));
            this.checkReachable = false;
        }

        @Override
        public void createResult() {
            super.createResult();
            this.cost.set(0);
        }

        @Override
        public void removed(Player player) {}

        @Override
        protected void clearContainer(Player player, Container container) {}
    }
}
