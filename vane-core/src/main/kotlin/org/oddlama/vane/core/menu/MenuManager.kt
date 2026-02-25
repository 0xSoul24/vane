package org.oddlama.vane.core.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.functional.Consumer2
import org.oddlama.vane.core.module.Context
import java.util.*

class MenuManager(context: Context<Core?>) : Listener<Core?>(context.namespace("Menus")) {
    private val openMenus = HashMap<UUID?, Menu?>()
    private val menus = HashMap<Inventory?, Menu?>()

    @JvmField
    var itemSelectorAccept: TranslatedItemStack<*>?
    @JvmField
    var itemSelectorCancel: TranslatedItemStack<*>?
    @JvmField
    var itemSelectorSelected: TranslatedItemStack<*>?

    @JvmField
    var genericSelectorPage: TranslatedItemStack<*>?
    @JvmField
    var genericSelectorCurrentPage: TranslatedItemStack<*>?
    @JvmField
    var genericSelectorFilter: TranslatedItemStack<*>?
    @JvmField
    var genericSelectorCancel: TranslatedItemStack<*>?

    @JvmField
    var headSelector: HeadSelectorGroup?

    init {
        val ctx = getContext()
        headSelector = HeadSelectorGroup(ctx!!)

        val ctxItemSelector = ctx.namespace("ItemSelector", "Menu configuration for item selector menus.")
        itemSelectorAccept = TranslatedItemStack<Core?>(
            ctxItemSelector,
            "Accept",
            Material.LIME_TERRACOTTA,
            1,
            "Used to confirm item selection."
        )
        itemSelectorCancel = TranslatedItemStack<Core?>(
            ctxItemSelector,
            "Cancel",
            Material.RED_TERRACOTTA,
            1,
            "Used to cancel item selection."
        )
        itemSelectorSelected = TranslatedItemStack<Core?>(
            ctxItemSelector,
            "Selected",
            Material.BARRIER,
            1,
            "Represents the selected item. Left-clicking will reset the selection to the initial value, and right-clicking will clear the selected item. The given stack is used as the 'empty', cleared item."
        )

        val ctxGenericSelector = ctx.namespace(
            "GenericSelector",
            "Menu configuration for generic selector menus."
        )
        genericSelectorPage = TranslatedItemStack<Core?>(
            ctxGenericSelector,
            "Page",
            Material.PAPER,
            1,
            "Used to select pages."
        )
        genericSelectorCurrentPage = TranslatedItemStack<Core?>(
            ctxGenericSelector,
            "CurrentPage",
            Material.MAP,
            1,
            "Used to indicate current page."
        )
        genericSelectorFilter = TranslatedItemStack<Core?>(
            ctxGenericSelector,
            "Filter",
            Material.HOPPER,
            1,
            "Used to filter items."
        )
        genericSelectorCancel = TranslatedItemStack<Core?>(
            ctxGenericSelector,
            "Cancel",
            Material.PRISMARINE_SHARD,
            1,
            "Used to cancel selection."
        )
    }

    fun menuFor(player: Player, view: InventoryView): Menu? {
        return menuFor(player, view.topInventory)
    }

    fun menuFor(player: Player, inventory: Inventory?): Menu? {
        val menu = menus[inventory]
        val open = openMenus[player.uniqueId]
        if (open !== menu && menu != null) {
            module!!
                .log.warning(
                    "Menu inconsistency: entity " +
                            player +
                            " accessed a menu '" +
                            openMenus[player.uniqueId] +
                            "' that isn't registered to it. The registered menu is '" +
                            menu +
                            "'"
                )
            return menu
        }
        return menu ?: open
    }

    fun add(player: Player, menu: Menu) {
        openMenus[player.uniqueId] = menu
        menus[menu.inventory()] = menu
    }

    fun remove(player: Player, menu: Menu) {
        openMenus.remove(player.uniqueId)
        val orphaned = openMenus.values.stream().allMatch { m: Menu? -> m !== menu }

        // Remove orphaned menus from other maps
        if (orphaned) {
            menus.remove(menu.inventory())
        }
    }

    fun forEachOpen(functor: Consumer2<Player?, Menu?>) {
        for (player in module!!.server.onlinePlayers) {
            val open = openMenus[player.uniqueId] ?: continue

            functor.apply(player, open)
        }
    }

    fun update(menu: Menu?) {
        module!!
            .server
            .onlinePlayers
            .stream()
            .filter { p: Player -> openMenus[p.uniqueId] === menu }
            .forEach { p: Player -> p.updateInventory() }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val clicker = event.whoClicked
        if (clicker !is Player) {
            return
        }

        val menu = menuFor(clicker, event.view)
        if (menu != null) {
            event.isCancelled = true
            val slot = if (event.clickedInventory === menu.inventory()) event.slot else -1
            menu.click(clicker, event.getCurrentItem(), slot, event)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val clicker = event.whoClicked
        if (clicker !is Player) {
            return
        }

        val menu = menuFor(clicker, event.view)
        if (menu != null) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val human = event.player
        if (human !is Player) {
            return
        }

        val menu = menuFor(human, event.view)
        menu?.closed(human, event.reason)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPrepareAnvilEvent(event: PrepareAnvilEvent) {
        val menu = menus[event.view.topInventory]
        if (menu != null) {
            event.view.repairCost = 0
        }
    }
}
