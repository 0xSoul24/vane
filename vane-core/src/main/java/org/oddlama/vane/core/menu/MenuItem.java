package org.oddlama.vane.core.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.functional.Function4;
import org.oddlama.vane.core.menu.Menu.ClickResult;

public class MenuItem implements MenuWidget {

    private int slot;
    private Function4<Player, Menu, MenuItem, InventoryClickEvent, ClickResult> onClick;
    private ItemStack item;
    private boolean autoUpdate;

    public MenuItem(int slot, final ItemStack item) {
        this(slot, item, (Function4<Player, Menu, MenuItem, InventoryClickEvent, ClickResult>) null);
    }

    public MenuItem(int slot, final ItemStack item, final Function3<Player, Menu, MenuItem, ClickResult> onClick) {
        this(slot, item, (player, menu, self, event) -> {
            if (!Menu.isLeftClick(event)) {
                return ClickResult.INVALID_CLICK;
            }
            return onClick.apply(player, menu, self);
        });
    }

    public MenuItem(
        int slot,
        final ItemStack item,
        final Function4<Player, Menu, MenuItem, InventoryClickEvent, ClickResult> onClick
    ) {
        this.slot = slot;
        this.onClick = onClick;
        autoUpdate = item == null;
        item(item);
    }

    public int slot() {
        return slot;
    }

    public ItemStack item(final Menu menu) {
        return menu.inventory().getItem(slot);
    }

    public void item(final ItemStack item) {
        this.item = item;
    }

    public void updateItem(final Menu menu, final ItemStack item) {
        this.item(item);
        menu.update();
    }

    @Override
    public boolean update(final Menu menu) {
        if (autoUpdate) {
            this.item((ItemStack) null);
        }

        final var cur = item(menu);
        if (cur != item) {
            menu.inventory().setItem(slot(), item);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ClickResult click(
        final Player player,
        final Menu menu,
        final ItemStack item,
        int slot,
        final InventoryClickEvent event
    ) {
        if (this.slot != slot) {
            return ClickResult.IGNORE;
        }

        if (onClick != null) {
            return onClick.apply(player, menu, this, event);
        } else {
            return ClickResult.IGNORE;
        }
    }
}
