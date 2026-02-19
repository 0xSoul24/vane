package org.oddlama.vane.core.menu;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.module.Context;

public class MenuManager extends Listener<Core> {

    private final HashMap<UUID, Menu> openMenus = new HashMap<>();
    private final HashMap<Inventory, Menu> menus = new HashMap<>();

    public TranslatedItemStack<?> itemSelectorAccept;
    public TranslatedItemStack<?> itemSelectorCancel;
    public TranslatedItemStack<?> itemSelectorSelected;

    public TranslatedItemStack<?> genericSelectorPage;
    public TranslatedItemStack<?> genericSelectorCurrentPage;
    public TranslatedItemStack<?> genericSelectorFilter;
    public TranslatedItemStack<?> genericSelectorCancel;

    public HeadSelectorGroup headSelector;

    public MenuManager(Context<Core> context) {
        super(context.namespace("Menus"));
        final var ctx = getContext();
        headSelector = new HeadSelectorGroup(ctx);

        final var ctxItemSelector = ctx.namespace("ItemSelector", "Menu configuration for item selector menus.");
        itemSelectorAccept = new TranslatedItemStack<>(
            ctxItemSelector,
            "Accept",
            Material.LIME_TERRACOTTA,
            1,
            "Used to confirm item selection."
        );
        itemSelectorCancel = new TranslatedItemStack<>(
            ctxItemSelector,
            "Cancel",
            Material.RED_TERRACOTTA,
            1,
            "Used to cancel item selection."
        );
        itemSelectorSelected = new TranslatedItemStack<>(
            ctxItemSelector,
            "Selected",
            Material.BARRIER,
            1,
            "Represents the selected item. Left-clicking will reset the selection to the initial value, and right-clicking will clear the selected item. The given stack is used as the 'empty', cleared item."
        );

        final var ctxGenericSelector = ctx.namespace(
            "GenericSelector",
            "Menu configuration for generic selector menus."
        );
        genericSelectorPage = new TranslatedItemStack<>(
            ctxGenericSelector,
            "Page",
            Material.PAPER,
            1,
            "Used to select pages."
        );
        genericSelectorCurrentPage = new TranslatedItemStack<>(
            ctxGenericSelector,
            "CurrentPage",
            Material.MAP,
            1,
            "Used to indicate current page."
        );
        genericSelectorFilter = new TranslatedItemStack<>(
            ctxGenericSelector,
            "Filter",
            Material.HOPPER,
            1,
            "Used to filter items."
        );
        genericSelectorCancel = new TranslatedItemStack<>(
            ctxGenericSelector,
            "Cancel",
            Material.PRISMARINE_SHARD,
            1,
            "Used to cancel selection."
        );
    }

    public Menu menuFor(final Player player, final InventoryView view) {
        return menuFor(player, view.getTopInventory());
    }

    public Menu menuFor(final Player player, final Inventory inventory) {
        final var menu = menus.get(inventory);
        final var open = openMenus.get(player.getUniqueId());
        if (open != menu && menu != null) {
            getModule()
                .log.warning(
                    "Menu inconsistency: entity " +
                    player +
                    " accessed a menu '" +
                    openMenus.get(player.getUniqueId()) +
                    "' that isn't registered to it. The registered menu is '" +
                    menu +
                    "'"
                );
            return menu;
        }
        return menu == null ? open : menu;
    }

    public void add(final Player player, final Menu menu) {
        openMenus.put(player.getUniqueId(), menu);
        menus.put(menu.inventory(), menu);
    }

    public void remove(final Player player, final Menu menu) {
        openMenus.remove(player.getUniqueId());
        final var orphaned = openMenus.values().stream().allMatch(m -> m != menu);

        // Remove orphaned menus from other maps
        if (orphaned) {
            menus.remove(menu.inventory());
        }
    }

    public void forEachOpen(final Consumer2<Player, Menu> functor) {
        for (final var player : getModule().getServer().getOnlinePlayers()) {
            final var open = openMenus.get(player.getUniqueId());
            if (open == null) {
                continue;
            }

            functor.apply(player, open);
        }
    }

    public void update(final Menu menu) {
        getModule()
            .getServer()
            .getOnlinePlayers()
            .stream()
            .filter(p -> openMenus.get(p.getUniqueId()) == menu)
            .forEach(p -> p.updateInventory());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        final var clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) {
            return;
        }

        final var player = (Player) clicker;
        final var menu = menuFor(player, event.getView());
        if (menu != null) {
            event.setCancelled(true);
            final var slot = event.getClickedInventory() == menu.inventory() ? event.getSlot() : -1;
            menu.click(player, event.getCurrentItem(), slot, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        final var clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) {
            return;
        }

        final var player = (Player) clicker;
        final var menu = menuFor(player, event.getView());
        if (menu != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(final InventoryCloseEvent event) {
        final var human = event.getPlayer();
        if (!(human instanceof Player)) {
            return;
        }

        final var player = (Player) human;
        final var menu = menuFor(player, event.getView());
        if (menu != null) {
            menu.closed(player, event.getReason());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrepareAnvilEvent(final PrepareAnvilEvent event) {
        final var menu = menus.get(event.getView().getTopInventory());
        if (menu != null) {
            event.getView().setRepairCost(0);
        }
    }
}
