package org.oddlama.vane.core.menu

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.functional.Function4

open class MenuItem @JvmOverloads constructor(
    private val slot: Int,
    item: ItemStack?,
    private val onClick: Function4<Player?, Menu?, MenuItem?, InventoryClickEvent?, Menu.ClickResult?>? = null as Function4<Player?, Menu?, MenuItem?, InventoryClickEvent?, Menu.ClickResult?>?
) : MenuWidget {
    private var item: ItemStack? = null
    private val autoUpdate: Boolean = item == null

    constructor(slot: Int, item: ItemStack?, onClick: Function3<Player?, Menu?, MenuItem?, Menu.ClickResult?>) : this(
        slot,
        item,
        Function4 { player: Player?, menu: Menu?, self: MenuItem?, event: InventoryClickEvent? ->
            if (!Menu.isLeftClick(event)) {
                Menu.ClickResult.INVALID_CLICK
            } else {
                onClick.apply(player, menu, self)
            }
        })

    init {
        item(item)
    }

    fun slot(): Int {
        return slot
    }

    fun item(menu: Menu?): ItemStack? {
        return menu?.inventory()?.getItem(slot)
    }

    open fun item(item: ItemStack?) {
        this.item = item
    }

    fun updateItem(menu: Menu, item: ItemStack?) {
        this.item(item)
        menu.update()
    }

    override fun update(menu: Menu?): Boolean {
        if (menu == null) return false

        if (autoUpdate) {
            this.item(null as ItemStack?)
        }

        val cur = item(menu)
        if (cur !== item) {
            menu.inventory()!!.setItem(slot(), item)
            return true
        } else {
            return false
        }
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
            onClick.apply(player, menu, this, event)
        } else {
            Menu.ClickResult.IGNORE
        }
    }
}
