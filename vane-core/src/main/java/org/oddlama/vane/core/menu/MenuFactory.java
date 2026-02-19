package org.oddlama.vane.core.menu;

import static org.oddlama.vane.util.ItemUtil.nameItem;
import static org.oddlama.vane.util.ItemUtil.nameOf;

import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.functional.Function4;
import org.oddlama.vane.core.material.HeadMaterial;
import org.oddlama.vane.core.material.HeadMaterialLibrary;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.module.Context;

public class MenuFactory {

    public static Menu anvilStringInput(
        final Context<?> context,
        final Player player,
        final String title,
        final ItemStack inputItemStack,
        final String defaultName,
        final Function3<Player, Menu, String, ClickResult> onClick
    ) {
        final var inputItem = inputItemStack.clone();
        final var meta = inputItem.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(defaultName));
        inputItem.setItemMeta(meta);

        final var anvil = new AnvilMenu(context, player, title);
        anvil.add(new MenuItem(0, inputItem));
        anvil.add(new MenuItemClickListener(2, (p, menu, item) -> onClick.apply(p, menu, nameOf(item))));
        return anvil;
    }

    public static Menu confirm(
        final Context<?> context,
        final String title,
        final ItemStack itemConfirm,
        final Function1<Player, ClickResult> onConfirm,
        final ItemStack itemCancel,
        final Consumer1<Player> onCancel
    ) {
        final var columns = 9;
        final var confirmationMenu = new Menu(
            context,
            Bukkit.createInventory(null, columns, LegacyComponentSerializer.legacySection().deserialize(title))
        );
        final var confirmIndex = (int) (Math.random() * columns);

        for (int i = 0; i < columns; ++i) {
            if (i == confirmIndex) {
                confirmationMenu.add(
                    new MenuItem(i, itemConfirm, (player, menu, self) -> {
                        menu.close(player);
                        return onConfirm.apply(player);
                    })
                );
            } else {
                confirmationMenu.add(
                    new MenuItem(i, itemCancel, (player, menu, self) -> {
                        menu.close(player);
                        onCancel.apply(player);
                        return ClickResult.SUCCESS;
                    })
                );
            }
        }

        // On natural close call cancel
        confirmationMenu.onNaturalClose(onCancel);

        return confirmationMenu;
    }

    public static Menu itemSelector(
        final Context<?> context,
        final Player player,
        final String title,
        @Nullable final ItemStack initialItem,
        boolean allowNothing,
        final Function2<Player, ItemStack, ClickResult> onConfirm,
        final Consumer1<Player> onCancel
    ) {
        return itemSelector(context, player, title, initialItem, allowNothing, onConfirm, onCancel, i -> i);
    }

    public static Menu itemSelector(
        final Context<?> context,
        final Player player,
        final String title,
        @Nullable final ItemStack initialItem,
        boolean allowNothing,
        final Function2<Player, ItemStack, ClickResult> onConfirm,
        final Consumer1<Player> onCancel,
        final Function1<ItemStack, ItemStack> onSelectItem
    ) {
        final var menuManager = context.getModule().core.menuManager;
        final Function1<ItemStack, ItemStack> setItemName = item ->
            nameItem(
                item,
                menuManager.itemSelectorSelected.langName.format(),
                menuManager.itemSelectorSelected.langLore.format()
            );

        final var noItem = setItemName.apply(new ItemStack(Material.BARRIER));
        final ItemStack defaultItem;
        if (initialItem == null || initialItem.getType() == Material.AIR) {
            defaultItem = noItem;
        } else {
            defaultItem = initialItem;
        }

        final var columns = 9;
        final var itemSelectorMenu = new Menu(
            context,
            Bukkit.createInventory(null, columns, LegacyComponentSerializer.legacySection().deserialize(title))
        );
        final var selectedItem = new MenuItem(4, defaultItem, (p, menu, self, event) -> {
            if (!Menu.isLeftOrRightClick(event)) {
                return ClickResult.INVALID_CLICK;
            }

            if (allowNothing && event.getClick() == ClickType.RIGHT) {
                // Clear selection
                self.updateItem(menu, noItem);
            } else {
                // Reset selection
                self.updateItem(menu, defaultItem);
            }
            return ClickResult.SUCCESS;
        }) {
            public ItemStack originalSelected = null;

            @Override
            public void item(final ItemStack item) {
                this.originalSelected = item;
                super.item(setItemName.apply(item.clone()));
            }
        };

        // Selected item, begin with default selected
        selectedItem.item(defaultItem);
        itemSelectorMenu.add(selectedItem);

        // Inventory listener
        itemSelectorMenu.add(
            new MenuItemClickListener(-1, (p, menu, item) -> {
                // Called when any item in inventory is clicked
                if (item == null) {
                    return ClickResult.IGNORE;
                }

                // Call on_select and check if the resulting item is valid
                item = onSelectItem.apply(item.clone());
                if (item == null) {
                    return ClickResult.ERROR;
                }

                selectedItem.item(item);
                menu.update();
                return ClickResult.SUCCESS;
            })
        );

        // Accept item
        itemSelectorMenu.add(
            new MenuItem(2, menuManager.itemSelectorAccept.item(), (p, menu, self) -> {
                final ItemStack item;
                if (selectedItem.originalSelected == noItem) {
                    if (allowNothing) {
                        item = null;
                    } else {
                        return ClickResult.ERROR;
                    }
                } else {
                    item = selectedItem.originalSelected;
                }

                menu.close(p);
                return onConfirm.apply(p, item);
            })
        );

        // Cancel item
        itemSelectorMenu.add(
            new MenuItem(6, menuManager.itemSelectorCancel.item(), (p, menu, self) -> {
                menu.close(p);
                onCancel.apply(player);
                return ClickResult.SUCCESS;
            })
        );

        // On natural close call cancel
        itemSelectorMenu.onNaturalClose(onCancel);

        return itemSelectorMenu;
    }

    public static <T, F extends Filter<T>> Menu genericSelector(
        final Context<?> context,
        final Player player,
        final String title,
        final String filterTitle,
        final List<T> things,
        final Function1<T, ItemStack> toItem,
        final F filter,
        final Function3<Player, Menu, T, ClickResult> onClick,
        final Consumer1<Player> onCancel
    ) {
        return genericSelector(
            context,
            player,
            title,
            filterTitle,
            things,
            toItem,
            filter,
            (p, menu, t, event) -> {
                if (!Menu.isLeftClick(event)) {
                    return ClickResult.INVALID_CLICK;
                }
                return onClick.apply(p, menu, t);
            },
            onCancel
        );
    }

    public static <T, F extends Filter<T>> Menu genericSelector(
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
        return GenericSelector.create(
            context,
            player,
            title,
            filterTitle,
            things,
            toItem,
            filter,
            onClick,
            onCancel
        );
    }

    public static Menu headSelector(
        final Context<?> context,
        final Player player,
        final Function3<Player, Menu, HeadMaterial, ClickResult> onClick,
        final Consumer1<Player> onCancel
    ) {
        return headSelector(
            context,
            player,
            (p, menu, t, event) -> {
                if (!Menu.isLeftClick(event)) {
                    return ClickResult.INVALID_CLICK;
                }
                return onClick.apply(p, menu, t);
            },
            onCancel
        );
    }

    public static Menu headSelector(
        final Context<?> context,
        final Player player,
        final Function4<Player, Menu, HeadMaterial, InventoryClickEvent, ClickResult> onClick,
        final Consumer1<Player> onCancel
    ) {
        final var menuManager = context.getModule().core.menuManager;
        final var allHeads = HeadMaterialLibrary.all()
            .stream()
            .sorted((a, b) -> a.key().toString().compareToIgnoreCase(b.key().toString()))
            .collect(Collectors.toList());

        final var filter = new HeadFilter();
        return MenuFactory.genericSelector(
            context,
            player,
            menuManager.headSelector.langTitle.str("§5§l" + allHeads.size()),
            menuManager.headSelector.langFilterTitle.str(),
            allHeads,
            h ->
                menuManager.headSelector.itemSelectHead.alternative(
                    h.item(),
                    "§a§l" + h.name(),
                    "§6" + h.category(),
                    "§b" + h.tags()
                ),
            filter,
            onClick,
            onCancel
        );
    }
}
