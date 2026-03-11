package org.oddlama.vane.core.menu

import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.functional.Consumer2
import org.oddlama.vane.core.module.Context
import java.util.logging.Level

open class Menu {
    protected val manager: MenuManager
    protected var inventory: Inventory? = null
    private val widgets: MutableSet<MenuWidget> = mutableSetOf()
    var onClose: Consumer2<Player?, InventoryCloseEvent.Reason?>? = null
        private set
    var onNaturalClose: Consumer1<Player?>? = null
        private set
    private var tag: Any? = null
    protected var tainted: Boolean = false

    protected constructor(context: Context<*>) {
        manager = context.module!!.core!!.menuManager!!
    }

    constructor(context: Context<*>, inventory: Inventory?) {
        manager = context.module!!.core!!.menuManager!!
        this.inventory = inventory
    }

    fun manager(): MenuManager = manager
    fun inventory(): Inventory? = inventory
    fun tag(): Any? = tag

    fun tag(tag: Any?): Menu = apply { this.tag = tag }
    fun taint() { tainted = true }

    fun add(widget: MenuWidget?) { widgets.add(widget!!) }
    fun remove(widget: MenuWidget?): Boolean = widgets.remove(widget)

    fun update() = update(false)

    open fun update(forceUpdate: Boolean) {
        val updated = widgets.count { it.update(this) }
        if (updated > 0 || forceUpdate) manager.update(this)
    }

    open fun openWindow(player: Player) {
        if (!tainted) player.openInventory(inventory!!)
    }

    fun open(player: Player) {
        if (tainted) return
        update(true)
        manager.scheduleNextTick {
            manager.add(player, this)
            openWindow(player)
        }
    }

    @JvmOverloads
    fun close(player: Player, reason: InventoryCloseEvent.Reason = InventoryCloseEvent.Reason.PLUGIN): Boolean {
        if (player.openInventory.topInventory !== inventory) {
            try {
                throw RuntimeException("Invalid close from unrelated menu.")
            } catch (e: RuntimeException) {
                manager.module!!.log.log(
                    Level.WARNING,
                    "Tried to close menu inventory that isn't opened by the player $player", e
                )
            }
            return false
        }
        manager.scheduleNextTick { player.closeInventory(reason) }
        return true
    }

    fun onClose(onClose: Consumer2<Player?, InventoryCloseEvent.Reason?>?): Menu = apply { this.onClose = onClose }
    fun onNaturalClose(onNaturalClose: Consumer1<Player?>?): Menu = apply { this.onNaturalClose = onNaturalClose }

    fun closed(player: Player, reason: InventoryCloseEvent.Reason?) {
        if (reason == InventoryCloseEvent.Reason.PLAYER) {
            onNaturalClose?.apply(player)
        } else {
            onClose?.apply(player, reason)
        }
        inventory!!.clear()
        manager.remove(player, this)
    }

    open fun onClick(player: Player?, item: ItemStack?, slot: Int, event: InventoryClickEvent?): ClickResult =
        ClickResult.IGNORE

    fun click(player: Player, item: ItemStack?, slot: Int, event: InventoryClickEvent) {
        if (event.action == InventoryAction.UNKNOWN) return

        var result = ClickResult.IGNORE
        result = ClickResult.or(result, onClick(player, item, slot, event))
        for (widget in widgets) {
            result = ClickResult.or(result, widget.click(player, this, item, slot, event) ?: ClickResult.IGNORE)
        }

        when (result) {
            ClickResult.INVALID_CLICK, ClickResult.IGNORE -> {}
            ClickResult.SUCCESS -> player.playSound(player.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1f, 1f)
            ClickResult.ERROR   -> player.playSound(player.location, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.MASTER, 1f, 1f)
        }
    }

    enum class ClickResult(private val priority: Int) {
        IGNORE(0), INVALID_CLICK(1), SUCCESS(2), ERROR(3);

        companion object {
            fun or(a: ClickResult, b: ClickResult): ClickResult = if (a.priority > b.priority) a else b
        }
    }

    companion object {
        @JvmStatic
        fun isLeftOrRightClick(event: InventoryClickEvent?): Boolean {
            val type = event?.click ?: return false
            return type == ClickType.LEFT || type == ClickType.RIGHT
        }

        @JvmStatic
        fun isLeftClick(event: InventoryClickEvent?): Boolean =
            event?.click == ClickType.LEFT
    }
}
