package org.oddlama.vane.core.menu

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.functional.Function1
import org.oddlama.vane.core.functional.Function4
import org.oddlama.vane.core.menu.Menu.Companion.isLeftClick
import org.oddlama.vane.core.menu.Menu.Companion.isLeftOrRightClick
import org.oddlama.vane.core.module.Context
import kotlin.math.abs
import kotlin.math.max

class GenericSelector<T, F : Filter<T?>?> private constructor() {
    private var menuManager: MenuManager? = null
    private var toItem: Function1<T?, ItemStack?>? = null
    private var onClick: Function4<Player?, Menu?, T?, InventoryClickEvent?, Menu.ClickResult?>? =
        null

    private var things: MutableList<T?>? = null
    private var filter: F? = null
    private var pageSize = 0

    private var updateFilter = true
    private var page = 0
    private var lastPage = 0
    private var filteredThings: MutableList<T?>? = null

    class PageSelector<T, F : Filter<T?>?>(
        private val genericSelector: GenericSelector<T?, F?>, // Inclusive
        private val slotFrom: Int,
        private val slotTo: Int
    ) : MenuWidget {

        // Shows page selector from [from, too)
        init {
            require(slotTo - slotFrom >= 3) { "PageSelector needs at least 3 assigned slots!" }
            require(((slotTo - slotFrom) % 2) != 0) { "PageSelector needs an uneven number of assigned slots!" }
        }

        override fun update(menu: Menu?): Boolean {
            if (menu == null) return false
            for (slot in slotFrom until slotTo) {
                val i = slot - slotFrom
                val offset = buttonOffset(i)
                val page = pageForOffset(offset)
                val noOp = page == genericSelector.page
                val actualOffset = page - genericSelector.page
                val item: ItemStack? = if (i == (slotTo - slotFrom) / 2) {
                    // Current page indicator
                    genericSelector.menuManager!!.genericSelectorCurrentPage!!.item(
                        "§6" + (page + 1),
                        "§6" + (genericSelector.lastPage + 1),
                        "§6" + genericSelector.filteredThings!!.size
                    )
                } else if (noOp) {
                    null
                } else {
                    genericSelector.menuManager!!.genericSelectorPage!!.itemAmount(
                        abs(actualOffset),
                        "§6" + (page + 1)
                    )
                }

                menu.inventory()!!.setItem(slot, item)
            }
            return true
        }

        private fun buttonOffset(i: Int): Int {
            if (i <= 0) {
                // Go back up to BIG_JUMP_SIZE pages
                return -BIG_JUMP_SIZE
            } else if (i >= (slotTo - slotFrom) - 1) {
                // Go forward up to BIG_JUMP_SIZE pages
                return BIG_JUMP_SIZE
            } else {
                val base = (slotTo - slotFrom) / 2
                return i - base
            }
        }

        private fun pageForOffset(offset: Int): Int {
            var page = genericSelector.page + offset
            if (page < 0) {
                page = 0
            } else if (page > genericSelector.lastPage) {
                page = genericSelector.lastPage
            }
            return page
        }

        override fun click(
            player: Player?,
            menu: Menu?,
            item: ItemStack?,
            slot: Int,
            event: InventoryClickEvent?
        ): Menu.ClickResult {
            if (menu == null) return Menu.ClickResult.IGNORE
            if (slot !in slotFrom until slotTo) {
                return Menu.ClickResult.IGNORE
            }

            if (menu.inventory()!!.getItem(slot) == null) {
                return Menu.ClickResult.IGNORE
            }

            if (!isLeftClick(event)) {
                return Menu.ClickResult.INVALID_CLICK
            }

            val offset = buttonOffset(slot - slotFrom)
            genericSelector.page = pageForOffset(offset)

            menu.update()
            return Menu.ClickResult.SUCCESS
        }

        companion object {
            private const val BIG_JUMP_SIZE = 5
        }
    }

    class SelectionArea<T, F : Filter<T?>?>(private val genericSelector: GenericSelector<T?, F?>,
                                            private val firstSlot: Int
    ) : MenuWidget {

        override fun update(menu: Menu?): Boolean {
            if (menu == null) return false
            for (i in 0 until genericSelector.pageSize) {
                val idx = genericSelector.page * genericSelector.pageSize + i
                if (idx >= genericSelector.filteredThings!!.size) {
                    menu.inventory()!!.setItem(firstSlot + i, null)
                } else {
                    menu
                        .inventory()!!
                        .setItem(
                            firstSlot + i,
                            genericSelector.toItem!!.apply(genericSelector.filteredThings!![idx])
                        )
                }
            }
            return true
        }

        override fun click(
            player: Player?,
            menu: Menu?,
            item: ItemStack?,
            slot: Int,
            event: InventoryClickEvent?
        ): Menu.ClickResult? {
            if (menu == null) return Menu.ClickResult.IGNORE
            if (slot < firstSlot || slot >= firstSlot + genericSelector.pageSize) {
                return Menu.ClickResult.IGNORE
            }

            if (menu.inventory()!!.getItem(slot) == null) {
                return Menu.ClickResult.IGNORE
            }

            val idx = genericSelector.page * genericSelector.pageSize + (slot - firstSlot)
            return genericSelector.onClick!!.apply(player, menu, genericSelector.filteredThings!![idx], event)
        }
    }

    companion object {
        fun <T, F : Filter<T?>?> create(
            context: Context<*>,
            player: Player?,
            title: String,
            filterTitle: String,
            things: MutableList<T?>?,
            toItem: Function1<T?, ItemStack?>,
            filter: F?,
            onClick: Function4<Player?, Menu?, T?, InventoryClickEvent?, Menu.ClickResult?>,
            onCancel: Consumer1<Player?>
        ): Menu {
            val columns = 9

            val genericSelector = GenericSelector<T?, F?>()
            genericSelector.menuManager = context.module!!.core!!.menuManager
            genericSelector.toItem = toItem
            genericSelector.onClick = onClick
            genericSelector.things = things
            genericSelector.filter = filter
            genericSelector.pageSize = 5 * columns

            val genericSelectorMenu: Menu = object : Menu(
                context,
                Bukkit.createInventory(null, 6 * columns, LegacyComponentSerializer.legacySection().deserialize(title))
            ) {
                override fun update(forceUpdate: Boolean) {
                    if (genericSelector.updateFilter) {
                        // Filter list before update
                        genericSelector.filteredThings = genericSelector.filter!!.filter(genericSelector.things)
                        genericSelector.page = 0
                        genericSelector.lastPage =
                            max(0, genericSelector.filteredThings!!.size - 1) / genericSelector.pageSize
                        genericSelector.updateFilter = false
                    }
                    super.update(forceUpdate)
                }
            }

            // Selection area
            genericSelectorMenu.add(SelectionArea(genericSelector, 0))

            // Page selector
            genericSelectorMenu.add(
                PageSelector(genericSelector, genericSelector.pageSize + 1, genericSelector.pageSize + 8)
            )

            // Filter item
            genericSelectorMenu.add(
                MenuItem(
                    genericSelector.pageSize,
                    genericSelector.menuManager!!.genericSelectorFilter!!.item(),
                    Function4 { p: Player?, menu: Menu?, self: MenuItem?, event: InventoryClickEvent? ->
                        if (!isLeftOrRightClick(event)) {
                            return@Function4 Menu.ClickResult.INVALID_CLICK
                        }
                        if (event!!.click == ClickType.RIGHT) {
                            genericSelector.filter!!.reset()
                            genericSelector.updateFilter = true
                            menu!!.update()
                        } else {
                            menu!!.close(p!!)
                            genericSelector.filter!!.openFilterSettings(context, p, filterTitle, menu)
                            genericSelector.updateFilter = true
                        }
                        Menu.ClickResult.SUCCESS
                    }
                )
            )

            // Cancel item
            genericSelectorMenu.add(
                MenuItem(
                    genericSelector.pageSize + 8,
                    genericSelector.menuManager!!.genericSelectorCancel!!.item()
                ) { p: Player?, menu: Menu?, self: MenuItem? ->
                    menu!!.close(p!!)
                    onCancel.apply(player)
                    Menu.ClickResult.SUCCESS
                }
            )

            // On natural close call cancel
            genericSelectorMenu.onNaturalClose(onCancel)

            return genericSelectorMenu
        }
    }
}
