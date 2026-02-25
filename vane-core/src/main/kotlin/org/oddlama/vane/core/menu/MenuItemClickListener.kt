package org.oddlama.vane.core.menu

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.functional.Function4

class MenuItemClickListener(
    private val slot: Int,
    private val onClick: Function4<Player?, Menu?, ItemStack?, InventoryClickEvent?, Menu.ClickResult?>?
) : MenuWidget {
    constructor(slot: Int, onClick: Function3<Player?, Menu?, ItemStack?, Menu.ClickResult?>) : this(
        slot,
        Function4 { player: Player?, menu: Menu?, item: ItemStack?, event: InventoryClickEvent? ->
            if (!Menu.isLeftClick(event)) {
                Menu.ClickResult.INVALID_CLICK
            } else {
                onClick.apply(player, menu, item)
            }
        })

    fun slot(): Int {
        return slot
    }

    override fun update(menu: Menu?): Boolean {
        return false
    }

    override fun click(
        player: Player?,
        menu: Menu?,
        item: ItemStack?,
        slot: Int,
        event: InventoryClickEvent?
    ): Menu.ClickResult? {
        if (this.slot != slot) {
            return Menu.ClickResult.IGNORE
        }

        return if (onClick != null) {
            onClick.apply(player, menu, item, event)
        } else {
            Menu.ClickResult.IGNORE
        }
    }
}
