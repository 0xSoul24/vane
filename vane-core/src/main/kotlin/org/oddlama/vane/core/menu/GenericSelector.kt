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

class GenericSelector<T, F : Filter<T?>?> private constructor(
    val menuManager: MenuManager,
    val toItem: Function1<T?, ItemStack?>,
    val onClick: Function4<Player?, Menu?, T?, InventoryClickEvent?, Menu.ClickResult?>,
    val things: MutableList<T?>,
    val filter: F,
    val pageSize: Int
) {
    var updateFilter = true
    var page = 0
    var lastPage = 0
    var filteredThings: MutableList<T?> = mutableListOf()

    class PageSelector<T, F : Filter<T?>?>(
        private val gs: GenericSelector<T, F>,
        private val slotFrom: Int,
        private val slotTo: Int
    ) : MenuWidget {

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
                val noOp = page == gs.page
                val actualOffset = page - gs.page
                val item: ItemStack? = when {
                    i == (slotTo - slotFrom) / 2 -> gs.menuManager.genericSelectorCurrentPage!!.item(
                        "§6${page + 1}", "§6${gs.lastPage + 1}", "§6${gs.filteredThings.size}"
                    )
                    noOp -> null
                    else -> gs.menuManager.genericSelectorPage!!.itemAmount(abs(actualOffset), "§6${page + 1}")
                }
                menu.inventory()!!.setItem(slot, item)
            }
            return true
        }

        private fun buttonOffset(i: Int): Int = when {
            i <= 0 -> -BIG_JUMP_SIZE
            i >= (slotTo - slotFrom) - 1 -> BIG_JUMP_SIZE
            else -> i - (slotTo - slotFrom) / 2
        }

        private fun pageForOffset(offset: Int): Int =
            (gs.page + offset).coerceIn(0, gs.lastPage)

        override fun click(player: Player?, menu: Menu?, item: ItemStack?, slot: Int, event: InventoryClickEvent?): Menu.ClickResult {
            if (menu == null) return Menu.ClickResult.IGNORE
            if (slot !in slotFrom until slotTo) return Menu.ClickResult.IGNORE
            if (menu.inventory()!!.getItem(slot) == null) return Menu.ClickResult.IGNORE
            if (!isLeftClick(event)) return Menu.ClickResult.INVALID_CLICK
            gs.page = pageForOffset(buttonOffset(slot - slotFrom))
            menu.update()
            return Menu.ClickResult.SUCCESS
        }

        companion object {
            private const val BIG_JUMP_SIZE = 5
        }
    }

    class SelectionArea<T, F : Filter<T?>?>(
        private val gs: GenericSelector<T, F>,
        private val firstSlot: Int
    ) : MenuWidget {

        override fun update(menu: Menu?): Boolean {
            if (menu == null) return false
            for (i in 0 until gs.pageSize) {
                val idx = gs.page * gs.pageSize + i
                val item = gs.filteredThings.getOrNull(idx)?.let { gs.toItem.apply(it) }
                menu.inventory()!!.setItem(firstSlot + i, item)
            }
            return true
        }

        override fun click(player: Player?, menu: Menu?, item: ItemStack?, slot: Int, event: InventoryClickEvent?): Menu.ClickResult? {
            if (menu == null) return Menu.ClickResult.IGNORE
            if (slot < firstSlot || slot >= firstSlot + gs.pageSize) return Menu.ClickResult.IGNORE
            if (menu.inventory()!!.getItem(slot) == null) return Menu.ClickResult.IGNORE
            val idx = gs.page * gs.pageSize + (slot - firstSlot)
            return gs.onClick.apply(player, menu, gs.filteredThings[idx], event)
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
            val menuManager = requireNotNull(context.module!!.core!!.menuManager)

            @Suppress("UNCHECKED_CAST")
            val gs = GenericSelector(
                menuManager = menuManager,
                toItem = toItem,
                onClick = onClick,
                things = things ?: mutableListOf(),
                filter = filter as F,
                pageSize = 5 * columns
            )

            val genericSelectorMenu: Menu = object : Menu(
                context,
                Bukkit.createInventory(null, 6 * columns, LegacyComponentSerializer.legacySection().deserialize(title))
            ) {
                override fun update(forceUpdate: Boolean) {
                    if (gs.updateFilter) {
                        gs.filteredThings = gs.filter!!.filter(gs.things)
                        gs.page = 0
                        gs.lastPage = max(0, gs.filteredThings.size - 1) / gs.pageSize
                        gs.updateFilter = false
                    }
                    super.update(forceUpdate)
                }
            }

            genericSelectorMenu.add(SelectionArea(gs, 0))
            genericSelectorMenu.add(PageSelector(gs, gs.pageSize + 1, gs.pageSize + 8))

            // Filter item
            genericSelectorMenu.add(MenuItem(
                gs.pageSize,
                menuManager.genericSelectorFilter!!.item(),
                Function4 { p, menu, _, event ->
                    if (!isLeftOrRightClick(event)) return@Function4 Menu.ClickResult.INVALID_CLICK
                    if (event!!.click == ClickType.RIGHT) {
                        gs.filter!!.reset()
                        gs.updateFilter = true
                        menu!!.update()
                    } else {
                        menu!!.close(p!!)
                        gs.filter!!.openFilterSettings(context, p, filterTitle, menu)
                        gs.updateFilter = true
                    }
                    Menu.ClickResult.SUCCESS
                }
            ))

            // Cancel item
            genericSelectorMenu.add(MenuItem(
                gs.pageSize + 8,
                menuManager.genericSelectorCancel!!.item()
            ) { p, menu, _ ->
                menu!!.close(p!!)
                onCancel.apply(player)
                Menu.ClickResult.SUCCESS
            })

            genericSelectorMenu.onNaturalClose(onCancel)

            return genericSelectorMenu
        }
    }
}
