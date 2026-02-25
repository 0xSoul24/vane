package org.oddlama.vane.core.menu

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import org.bukkit.entity.Player
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.Nms.playerHandle

class AnvilMenu(context: Context<*>, player: Player, private val title: String) : Menu(context) {
    private val entity = playerHandle(player)
    private val container: AnvilContainer
    private val containerId: Int = entity!!.nextContainerCounter()

    init {
        this.container = AnvilContainer(containerId, entity!!)
        this.container.setTitle(Component.literal(title))
        this.inventory = container.bukkitView.topInventory
    }

    override fun openWindow(player: Player) {
        if (tainted) {
            return
        }

        if (playerHandle(player) !== entity) {
            manager
                .module!!
                .log.warning("AnvilMenu.open() was called with a player for whom this inventory wasn't created!")
        }

        entity!!.connection.send(
            ClientboundOpenScreenPacket(containerId, container.type, Component.literal(title))
        )
        entity.initMenu(container)
        entity.containerMenu = container
    }

    private inner class AnvilContainer(windowId: Int, entity: ServerPlayer) :
        AnvilMenu(windowId, entity.inventory, ContainerLevelAccess.create(entity.level(), BlockPos(0, 0, 0))) {
        init {
            this.checkReachable = false
        }

        override fun createResult() {
            super.createResult()
            this.cost.set(0)
        }

        override fun removed(player: net.minecraft.world.entity.player.Player) {}

        override fun clearContainer(player: net.minecraft.world.entity.player.Player, container: Container) {}
    }
}
