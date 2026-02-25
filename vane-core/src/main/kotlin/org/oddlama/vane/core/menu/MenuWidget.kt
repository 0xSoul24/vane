package org.oddlama.vane.core.menu

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

interface MenuWidget {
    fun update(menu: Menu?): Boolean

    fun click(
        player: Player?,
        menu: Menu?,
        item: ItemStack?,
        slot: Int,
        event: InventoryClickEvent?
    ): Menu.ClickResult?
}
