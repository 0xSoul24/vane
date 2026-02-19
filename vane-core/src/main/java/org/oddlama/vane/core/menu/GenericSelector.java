package org.oddlama.vane.core.menu;

import java.util.List;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.Function4;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.module.Context;

public class GenericSelector<T, F extends Filter<T>> {

    private MenuManager menuManager;
    private Function1<T, ItemStack> toItem;
    private Function4<Player, Menu, T, InventoryClickEvent, ClickResult> onClick;

    private List<T> things;
    private F filter;
    private int pageSize;

    private boolean updateFilter = true;
    private int page = 0;
    private int lastPage = 0;
    private List<T> filteredThings = null;

    private GenericSelector() {}

    public static <T, F extends Filter<T>> Menu create(
        final Context<?> context,
        final Player player,
        final String title,
        final String filterTitle,
        final List<T> things,
        final Function1<T, ItemStack> toItem,
        final F filter,
        final Function4<Player, Menu, T, InventoryClickEvent, ClickResult> onClick,
        final Consumer1<Player> onCancel
    ) {
        final var columns = 9;

        final var genericSelector = new GenericSelector<T, F>();
        genericSelector.menuManager = context.getModule().core.menuManager;
        genericSelector.toItem = toItem;
        genericSelector.onClick = onClick;
        genericSelector.things = things;
        genericSelector.filter = filter;
        genericSelector.pageSize = 5 * columns;

        final var genericSelectorMenu = new Menu(
            context,
            Bukkit.createInventory(null, 6 * columns, LegacyComponentSerializer.legacySection().deserialize(title))
        ) {
            @Override
            public void update(boolean forceUpdate) {
                if (genericSelector.updateFilter) {
                    // Filter list before update
                    genericSelector.filteredThings = genericSelector.filter.filter(genericSelector.things);
                    genericSelector.page = 0;
                    genericSelector.lastPage =
                        Math.max(0, genericSelector.filteredThings.size() - 1) / genericSelector.pageSize;
                    genericSelector.updateFilter = false;
                }
                super.update(forceUpdate);
            }
        };

        // Selection area
        genericSelectorMenu.add(new SelectionArea<>(genericSelector, 0));

        // Page selector
        genericSelectorMenu.add(
            new PageSelector<>(genericSelector, genericSelector.pageSize + 1, genericSelector.pageSize + 8)
        );

        // Filter item
        genericSelectorMenu.add(
            new MenuItem(
                genericSelector.pageSize,
                genericSelector.menuManager.genericSelectorFilter.item(),
                (p, menu, self, event) -> {
                    if (!Menu.isLeftOrRightClick(event)) {
                        return ClickResult.INVALID_CLICK;
                    }

                    if (event.getClick() == ClickType.RIGHT) {
                        genericSelector.filter.reset();
                        genericSelector.updateFilter = true;
                        menu.update();
                    } else {
                        menu.close(p);
                        genericSelector.filter.openFilterSettings(context, p, filterTitle, menu);
                        genericSelector.updateFilter = true;
                    }
                    return ClickResult.SUCCESS;
                }
            )
        );

        // Cancel item
        genericSelectorMenu.add(
            new MenuItem(
                genericSelector.pageSize + 8,
                genericSelector.menuManager.genericSelectorCancel.item(),
                (p, menu, self) -> {
                    menu.close(p);
                    onCancel.apply(player);
                    return ClickResult.SUCCESS;
                }
            )
        );

        // On natural close call cancel
        genericSelectorMenu.onNaturalClose(onCancel);

        return genericSelectorMenu;
    }

    public static class PageSelector<T, F extends Filter<T>> implements MenuWidget {

        private static final int BIG_JUMP_SIZE = 5;

        private final GenericSelector<T, F> genericSelector;
        private final int slotFrom; // Inclusive
        private final int slotTo; // Exclusive

        // Shows page selector from [from, too)
        public PageSelector(final GenericSelector<T, F> genericSelector, int slotFrom, int slotTo) {
            this.genericSelector = genericSelector;
            this.slotFrom = slotFrom;
            this.slotTo = slotTo;
            if (this.slotTo - this.slotFrom < 3) {
                throw new IllegalArgumentException("PageSelector needs at least 3 assigned slots!");
            }
            if (((this.slotTo - this.slotFrom) % 2) == 0) {
                throw new IllegalArgumentException("PageSelector needs an uneven number of assigned slots!");
            }
        }

        @Override
        public boolean update(final Menu menu) {
            for (int slot = slotFrom; slot < slotTo; ++slot) {
                final var i = slot - slotFrom;
                final var offset = buttonOffset(i);
                final var page = pageForOffset(offset);
                final var noOp = page == genericSelector.page;
                final var actualOffset = page - genericSelector.page;
                final ItemStack item;
                if (i == (slotTo - slotFrom) / 2) {
                    // Current page indicator
                    item = genericSelector.menuManager.genericSelectorCurrentPage.item(
                        "§6" + (page + 1),
                        "§6" + (genericSelector.lastPage + 1),
                        "§6" + genericSelector.filteredThings.size()
                    );
                } else if (noOp) {
                    item = null;
                } else {
                    item = genericSelector.menuManager.genericSelectorPage.itemAmount(
                        Math.abs(actualOffset),
                        "§6" + (page + 1)
                    );
                }

                menu.inventory().setItem(slot, item);
            }
            return true;
        }

        private int buttonOffset(int i) {
            if (i <= 0) {
                // Go back up to BIG_JUMP_SIZE pages
                return -BIG_JUMP_SIZE;
            } else if (i >= (slotTo - slotFrom) - 1) {
                // Go forward up to BIG_JUMP_SIZE pages
                return BIG_JUMP_SIZE;
            } else {
                final var base = (slotTo - slotFrom) / 2;
                return i - base;
            }
        }

        private int pageForOffset(int offset) {
            int page = genericSelector.page + offset;
            if (page < 0) {
                page = 0;
            } else if (page > genericSelector.lastPage) {
                page = genericSelector.lastPage;
            }
            return page;
        }

        @Override
        public ClickResult click(
            final Player player,
            final Menu menu,
            final ItemStack item,
            int slot,
            final InventoryClickEvent event
        ) {
            if (slot < slotFrom || slot >= slotTo) {
                return ClickResult.IGNORE;
            }

            if (menu.inventory().getItem(slot) == null) {
                return ClickResult.IGNORE;
            }

            if (!Menu.isLeftClick(event)) {
                return ClickResult.INVALID_CLICK;
            }

            final var offset = buttonOffset(slot - slotFrom);
            genericSelector.page = pageForOffset(offset);

            menu.update();
            return ClickResult.SUCCESS;
        }
    }

    public static class SelectionArea<T, F extends Filter<T>> implements MenuWidget {

        private final GenericSelector<T, F> genericSelector;
        private final int firstSlot;

        public SelectionArea(final GenericSelector<T, F> genericSelector, final int firstSlot) {
            this.genericSelector = genericSelector;
            this.firstSlot = firstSlot;
        }

        @Override
        public boolean update(final Menu menu) {
            for (int i = 0; i < genericSelector.pageSize; ++i) {
                final var idx = genericSelector.page * genericSelector.pageSize + i;
                if (idx >= genericSelector.filteredThings.size()) {
                    menu.inventory().setItem(firstSlot + i, null);
                } else {
                    menu
                        .inventory()
                        .setItem(
                            firstSlot + i,
                            genericSelector.toItem.apply(genericSelector.filteredThings.get(idx))
                        );
                }
            }
            return true;
        }

        @Override
        public ClickResult click(
            final Player player,
            final Menu menu,
            final ItemStack item,
            int slot,
            final InventoryClickEvent event
        ) {
            if (slot < firstSlot || slot >= firstSlot + genericSelector.pageSize) {
                return ClickResult.IGNORE;
            }

            if (menu.inventory().getItem(slot) == null) {
                return ClickResult.IGNORE;
            }

            final var idx = genericSelector.page * genericSelector.pageSize + (slot - firstSlot);
            return genericSelector.onClick.apply(player, menu, genericSelector.filteredThings.get(idx), event);
        }
    }
}
