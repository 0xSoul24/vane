package org.oddlama.vane.core.menu;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.module.Context;

public class Menu {

    protected final MenuManager manager;
    protected Inventory inventory = null;
    private final Set<MenuWidget> widgets = new HashSet<>();
    private Consumer2<Player, InventoryCloseEvent.Reason> onClose = null;
    private Consumer1<Player> onNaturalClose = null;
    private Object tag = null;

    // A tainted menu will refuse to be opened.
    // Useful to prevent an invalid menu from reopening
    // after its state has been captured.
    protected boolean tainted = false;

    protected Menu(final Context<?> context) {
        this.manager = context.getModule().core.menuManager;
    }

    public Menu(final Context<?> context, final Inventory inventory) {
        this.manager = context.getModule().core.menuManager;
        this.inventory = inventory;
    }

    public MenuManager manager() {
        return manager;
    }

    public Inventory inventory() {
        return inventory;
    }

    public Object tag() {
        return tag;
    }

    public Menu tag(Object tag) {
        this.tag = tag;
        return this;
    }

    public void taint() {
        this.tainted = true;
    }

    public void add(final MenuWidget widget) {
        widgets.add(widget);
    }

    public boolean remove(final MenuWidget widget) {
        return widgets.remove(widget);
    }

    public void update() {
        update(false);
    }

    public void update(boolean forceUpdate) {
        int updated = widgets.stream().mapToInt(w -> w.update(this) ? 1 : 0).sum();

        if (updated > 0 || forceUpdate) {
            // Send inventory content to players
            manager.update(this);
        }
    }

    public void openWindow(final Player player) {
        if (tainted) {
            return;
        }
        player.openInventory(inventory);
    }

    public final void open(final Player player) {
        if (tainted) {
            return;
        }
        update(true);
        manager.scheduleNextTick(() -> {
            manager.add(player, this);
            openWindow(player);
        });
    }

    public boolean close(final Player player) {
        return close(player, InventoryCloseEvent.Reason.PLUGIN);
    }

    public boolean close(final Player player, final InventoryCloseEvent.Reason reason) {
        final var topInventory = player.getOpenInventory().getTopInventory();
        if (topInventory != inventory) {
            try {
                throw new RuntimeException("Invalid close from unrelated menu.");
            } catch (RuntimeException e) {
                manager
                    .getModule()
                    .log.log(
                        Level.WARNING,
                        "Tried to close menu inventory that isn't opened by the player " + player,
                        e
                    );
            }
            return false;
        }

        manager.scheduleNextTick(() -> player.closeInventory(reason));
        return true;
    }

    public Consumer2<Player, InventoryCloseEvent.Reason> getOnClose() {
        return onClose;
    }

    public Menu onClose(final Consumer2<Player, InventoryCloseEvent.Reason> onClose) {
        this.onClose = onClose;
        return this;
    }

    public Consumer1<Player> getOnNaturalClose() {
        return onNaturalClose;
    }

    public Menu onNaturalClose(final Consumer1<Player> onNaturalClose) {
        this.onNaturalClose = onNaturalClose;
        return this;
    }

    public final void closed(final Player player, final InventoryCloseEvent.Reason reason) {
        if (reason == InventoryCloseEvent.Reason.PLAYER && onNaturalClose != null) {
            onNaturalClose.apply(player);
        } else {
            if (onClose != null) {
                onClose.apply(player, reason);
            }
        }
        inventory.clear();
        manager.remove(player, this);
    }

    public ClickResult onClick(final Player player, final ItemStack item, int slot, final InventoryClickEvent event) {
        return ClickResult.IGNORE;
    }

    public final void click(final Player player, final ItemStack item, int slot, final InventoryClickEvent event) {
        // Ignore unknown click actions
        if (event.getAction() == InventoryAction.UNKNOWN) {
            return;
        }

        // Send event to this menu
        var result = ClickResult.IGNORE;
        result = ClickResult.or(result, onClick(player, item, slot, event));

        // Send event to all widgets
        for (final var widget : widgets) {
            result = ClickResult.or(result, widget.click(player, this, item, slot, event));
        }

        switch (result) {
            default:
            case INVALID_CLICK:
            case IGNORE:
                break;
            case SUCCESS:
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                break;
            case ERROR:
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.MASTER, 1.0f, 1.0f);
                break;
        }
    }

    public static boolean isLeftOrRightClick(final InventoryClickEvent event) {
        final var type = event.getClick();
        return type == ClickType.LEFT || type == ClickType.RIGHT;
    }

    public static boolean isLeftClick(final InventoryClickEvent event) {
        final var type = event.getClick();
        return type == ClickType.LEFT;
    }

    public static enum ClickResult {
        IGNORE(0),
        INVALID_CLICK(1),
        SUCCESS(2),
        ERROR(3);

        private int priority;

        private ClickResult(int priority) {
            this.priority = priority;
        }

        public static ClickResult or(final ClickResult a, final ClickResult b) {
            return a.priority > b.priority ? a : b;
        }
    }
}
