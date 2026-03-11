package org.oddlama.vane.core.menu

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.functional.Function4

open class MenuItem @JvmOverloads constructor(
    private val slot: Int,
    item: ItemStack?,
    private val onClick: Function4<Player?, Menu?, MenuItem?, InventoryClickEvent?, Menu.ClickResult?>? = null
) : MenuWidget {
    private var item: ItemStack? = null
    private val autoUpdate: Boolean = item == null

    constructor(slot: Int, item: ItemStack?, onClick: Function3<Player?, Menu?, MenuItem?, Menu.ClickResult?>) : this(
        slot, item,
        Function4 { player, menu, self, event ->
            if (!Menu.isLeftClick(event)) Menu.ClickResult.INVALID_CLICK
            else onClick.apply(player, menu, self)
        }
    )

    init { item(item) }

    fun slot(): Int = slot

    fun item(menu: Menu?): ItemStack? = menu?.inventory()?.getItem(slot)

    open fun item(item: ItemStack?) { this.item = item }

    fun updateItem(menu: Menu, item: ItemStack?) {
        this.item(item)
        menu.update()
    }

    override fun update(menu: Menu?): Boolean {
        if (menu == null) return false
        if (autoUpdate) item(null as ItemStack?)
        val cur = item(menu)
        return if (cur !== item) {
            menu.inventory()!!.setItem(slot(), item)
            true
        } else false
    }

    override fun click(player: Player?, menu: Menu?, item: ItemStack?, slot: Int, event: InventoryClickEvent?): Menu.ClickResult? {
        if (this.slot != slot) return Menu.ClickResult.IGNORE
        return onClick?.apply(player, menu, this, event) ?: Menu.ClickResult.IGNORE
    }
}
