package org.oddlama.vane.regions.menu;

import static org.oddlama.vane.util.PlayerUtil.giveItems;

import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Filter;
import org.oddlama.vane.core.menu.Menu;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.menu.MenuItem;
import org.oddlama.vane.core.menu.MenuWidget;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.regions.Regions;
import org.oddlama.vane.regions.region.Region;
import org.oddlama.vane.regions.region.RegionGroup;
import org.oddlama.vane.regions.region.RegionSelection;
import org.oddlama.vane.util.StorageUtil;

public class RegionMenu extends ModuleComponent<Regions> {

    @LangMessage
    public TranslatedMessage langTitle;

    @LangMessage
    public TranslatedMessage langDeleteConfirmTitle;

    @LangMessage
    public TranslatedMessage langSelectRegionGroupTitle;

    @LangMessage
    public TranslatedMessage langFilterRegionGroupsTitle;

    public TranslatedItemStack<?> itemRename;
    public TranslatedItemStack<?> itemDelete;
    public TranslatedItemStack<?> itemDeleteConfirmAccept;
    public TranslatedItemStack<?> itemDeleteConfirmCancel;
    public TranslatedItemStack<?> itemAssignRegionGroup;
    public TranslatedItemStack<?> itemSelectRegionGroup;

    public RegionMenu(Context<Regions> context) {
        super(context.namespace("Region"));
        final var ctx = getContext();
        itemRename = new TranslatedItemStack<>(ctx, "Rename", Material.NAME_TAG, 1, "Used to rename the region.");
        itemDelete = new TranslatedItemStack<>(
            ctx,
            "Delete",
            StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
            1,
            "Used to delete this region."
        );
        itemDeleteConfirmAccept = new TranslatedItemStack<>(
            ctx,
            "DeleteConfirmAccept",
            StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
            1,
            "Used to confirm deleting the region."
        );
        itemDeleteConfirmCancel = new TranslatedItemStack<>(
            ctx,
            "DeleteConfirmCancel",
            Material.PRISMARINE_SHARD,
            1,
            "Used to cancel deleting the region."
        );
        itemAssignRegionGroup = new TranslatedItemStack<>(
            ctx,
            "AssignRegionGroup",
            Material.GLOBE_BANNER_PATTERN,
            1,
            "Used to assign a region group."
        );
        itemSelectRegionGroup = new TranslatedItemStack<>(
            ctx,
            "SelectRegionGroup",
            Material.GLOBE_BANNER_PATTERN,
            1,
            "Used to represent a region group in the region group assignment list."
        );
    }

    public Menu create(final Region region, final Player player) {
        final var columns = 9;
        final var title = langTitle.strComponent("§5§l" + region.name());
        final var regionMenu = new Menu(getContext(), Bukkit.createInventory(null, columns, title));
        regionMenu.tag(new RegionMenuTag(region.id()));

        if (getModule().mayAdministrate(player, region)) {
            regionMenu.add(menuItemRename(region));
            regionMenu.add(menuItemDelete(region));
            regionMenu.add(menuItemAssignRegionGroup(region));
        }

        regionMenu.onNaturalClose(player2 -> getModule().menus.mainMenu.create(player2).open(player2));

        return regionMenu;
    }

    private MenuWidget menuItemRename(final Region region) {
        return new MenuItem(0, itemRename.item(), (player, menu, self) -> {
            menu.close(player);
            if (!getModule().mayAdministrate(player, region)) {
                return ClickResult.ERROR;
            }

            getModule()
                .menus.enterRegionNameMenu.create(player, region.name(), (player2, name) -> {
                    region.name(name);

                    // Update map marker
                    getModule().updateMarker(region);

                    // Open new menu because of possibly changed title
                    getModule().menus.regionMenu.create(region, player2).open(player2);
                    return ClickResult.SUCCESS;
                })
                .onNaturalClose(player2 -> {
                    // Open new menu because of possibly changed title
                    getModule().menus.regionMenu.create(region, player2).open(player2);
                })
                .open(player);

            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemDelete(final Region region) {
        return new MenuItem(1, itemDelete.item(), (player, menu, self) -> {
            menu.close(player);
            MenuFactory.confirm(
                getContext(),
                langDeleteConfirmTitle.str(),
                itemDeleteConfirmAccept.item(),
                player2 -> {
                    if (!getModule().mayAdministrate(player2, region)) {
                        return ClickResult.ERROR;
                    }

                    getModule().removeRegion(region);

                    // Give back money
                    final var tempSel = new RegionSelection(getModule());
                    tempSel.primary = region.extent().min();
                    tempSel.secondary = region.extent().max();

                    final var price = tempSel.price();
                    if (getModule().configEconomyAsCurrency) {
                        final var transaction = getModule().economy.deposit(player2, price);
                        if (!transaction.transactionSuccess()) {
                            getModule()
                                .log.severe(
                                    "Player " +
                                    player2 +
                                    " deleted region '" +
                                    region.name() +
                                    "' (cost " +
                                    price +
                                    ") but the economy plugin failed to deposit:"
                                );
                            getModule().log.severe("Error message: " + transaction.errorMessage);
                        }
                    } else {
                        giveItems(player2, new ItemStack(getModule().configCurrency), (int) price);
                    }
                    return ClickResult.SUCCESS;
                },
                itemDeleteConfirmCancel.item(),
                player2 -> menu.open(player2)
            )
                .tag(new RegionMenuTag(region.id()))
                .open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemAssignRegionGroup(final Region region) {
        return new MenuItem(2, itemAssignRegionGroup.item(), (player, menu, self) -> {
            menu.close(player);
            final var allRegionGroups = getModule()
                .allRegionGroups()
                .stream()
                .filter(g -> getModule().mayAdministrate(player, g))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .collect(Collectors.toList());

            final var filter = new Filter.StringFilter<RegionGroup>((r, str) -> r.name().toLowerCase().contains(str));
            MenuFactory.genericSelector(
                getContext(),
                player,
                langSelectRegionGroupTitle.str(),
                langFilterRegionGroupsTitle.str(),
                allRegionGroups,
                r -> itemSelectRegionGroup.item("§a§l" + r.name()),
                filter,
                (player2, m, group) -> {
                    if (!getModule().mayAdministrate(player2, region)) {
                        return ClickResult.ERROR;
                    }

                    m.close(player2);
                    region.regionGroupId(group.id());
                    markPersistentStorageDirty();
                    menu.open(player2);
                    return ClickResult.SUCCESS;
                },
                player2 -> menu.open(player2)
            ).open(player);
            return ClickResult.SUCCESS;
        });
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
