package org.oddlama.vane.regions.menu;

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

public class MainMenu extends ModuleComponent<Regions> {

    @LangMessage
    public TranslatedMessage langTitle;

    @LangMessage
    public TranslatedMessage langSelectRegionTitle;

    @LangMessage
    public TranslatedMessage langFilterRegionsTitle;

    @LangMessage
    public TranslatedMessage langSelectRegionGroupTitle;

    @LangMessage
    public TranslatedMessage langFilterRegionGroupsTitle;

    public TranslatedItemStack<?> itemCreateRegionStartSelection;
    public TranslatedItemStack<?> itemCreateRegionInvalidSelection;
    public TranslatedItemStack<?> itemCreateRegionValidSelection;
    public TranslatedItemStack<?> itemCancelSelection;
    public TranslatedItemStack<?> itemCurrentRegion;
    public TranslatedItemStack<?> itemListRegions;
    public TranslatedItemStack<?> itemSelectRegion;
    public TranslatedItemStack<?> itemCreateRegionGroup;
    public TranslatedItemStack<?> itemCurrentRegionGroup;
    public TranslatedItemStack<?> itemListRegionGroups;
    public TranslatedItemStack<?> itemSelectRegionGroup;

    public MainMenu(Context<Regions> context) {
        super(context.namespace("Main"));
        final var ctx = getContext();
        itemCreateRegionStartSelection = new TranslatedItemStack<>(
            ctx,
            "CreateRegionStartSelection",
            Material.WRITABLE_BOOK,
            1,
            "Used to start creating a new region selection."
        );
        itemCreateRegionInvalidSelection = new TranslatedItemStack<>(
            ctx,
            "CreateRegionInvalidSelection",
            Material.BARRIER,
            1,
            "Used to indicate an invalid selection."
        );
        itemCreateRegionValidSelection = new TranslatedItemStack<>(
            ctx,
            "CreateRegionValidSelection",
            Material.WRITABLE_BOOK,
            1,
            "Used to create a new region with the current selection."
        );
        itemCancelSelection = new TranslatedItemStack<>(
            ctx,
            "CancelSelection",
            Material.RED_TERRACOTTA,
            1,
            "Used to cancel region selection."
        );
        itemListRegions = new TranslatedItemStack<>(
            ctx,
            "ListRegions",
            Material.COMPASS,
            1,
            "Used to select a region the player owns."
        );
        itemSelectRegion = new TranslatedItemStack<>(
            ctx,
            "SelectRegion",
            Material.FILLED_MAP,
            1,
            "Used to represent a region in the region selection list."
        );
        itemCurrentRegion = new TranslatedItemStack<>(
            ctx,
            "CurrentRegion",
            Material.FILLED_MAP,
            1,
            "Used to access the region the player currently stands in."
        );
        itemCreateRegionGroup = new TranslatedItemStack<>(
            ctx,
            "CreateRegionGroup",
            Material.WRITABLE_BOOK,
            1,
            "Used to create a new region group."
        );
        itemListRegionGroups = new TranslatedItemStack<>(
            ctx,
            "ListRegionGroups",
            Material.COMPASS,
            1,
            "Used to select a region group the player may administrate."
        );
        itemCurrentRegionGroup = new TranslatedItemStack<>(
            ctx,
            "CurrentRegionGroup",
            Material.GLOBE_BANNER_PATTERN,
            1,
            "Used to access the region group associated with the region the player currently stands in."
        );
        itemSelectRegionGroup = new TranslatedItemStack<>(
            ctx,
            "SelectRegionGroup",
            Material.GLOBE_BANNER_PATTERN,
            1,
            "Used to represent a region group in the region group selection list."
        );
    }

    public Menu create(final Player player) {
        final var columns = 9;
        final var title = langTitle.strComponent();
        final var mainMenu = new Menu(getContext(), Bukkit.createInventory(null, columns, title));

        final var selectionMode = getModule().isSelectingRegion(player);
        final var region = getModule().regionAt(player.getLocation());
        if (region != null) {
            mainMenu.tag(new RegionMenuTag(region.id()));
        }

        // Check if target selection would be allowed
        if (selectionMode) {
            final var selection = getModule().getRegionSelection(player);
            mainMenu.add(menuItemCreateRegion(player, selection));
            mainMenu.add(menuItemCancelSelection());
        } else {
            mainMenu.add(menuItemStartSelection());
            mainMenu.add(menuItemListRegions());
            if (region != null && getModule().mayAdministrate(player, region)) {
                mainMenu.add(menuItemCurrentRegion(region));
            }
        }

        mainMenu.add(menuItemCreateRegionGroup());
        mainMenu.add(menuItemListRegionGroups());
        if (region != null) {
            final var group = region.regionGroup(getModule());
            if (getModule().mayAdministrate(player, group)) {
                mainMenu.add(menuItemCurrentRegionGroup(group));
            }
        }

        return mainMenu;
    }

    private MenuWidget menuItemStartSelection() {
        return new MenuItem(0, itemCreateRegionStartSelection.item(), (player, menu, self) -> {
            menu.close(player);
            getModule().startRegionSelection(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemCancelSelection() {
        return new MenuItem(1, itemCancelSelection.item(), (player, menu, self) -> {
            menu.close(player);
            getModule().cancelRegionSelection(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemCreateRegion(final Player finalPlayer, final RegionSelection selection) {
        return new MenuItem(0, null, (player, menu, self) -> {
            if (selection.isValid(finalPlayer)) {
                menu.close(player);

                getModule()
                    .menus.enterRegionNameMenu.create(player, (player2, name) -> {
                        if (getModule().createRegionFromSelection(finalPlayer, name)) {
                            return ClickResult.SUCCESS;
                        } else {
                            return ClickResult.ERROR;
                        }
                    })
                    .onNaturalClose(player2 -> menu.open(player2))
                    .open(player);

                return ClickResult.SUCCESS;
            } else {
                return ClickResult.ERROR;
            }
        }) {
            @Override
            public void item(final ItemStack item) {
                if (selection.isValid(finalPlayer)) {
                    final var dx = 1 + Math.abs(selection.primary.getX() - selection.secondary.getX());
                    final var dy = 1 + Math.abs(selection.primary.getY() - selection.secondary.getY());
                    final var dz = 1 + Math.abs(selection.primary.getZ() - selection.secondary.getZ());
                    super.item(
                        itemCreateRegionValidSelection.item(
                            "§a" + dx,
                            "§a" + dy,
                            "§a" + dz,
                            "§b" + getModule().configMinRegionExtentX,
                            "§b" + getModule().configMinRegionExtentY,
                            "§b" + getModule().configMinRegionExtentZ,
                            "§b" + getModule().configMaxRegionExtentX,
                            "§b" + getModule().configMaxRegionExtentY,
                            "§b" + getModule().configMaxRegionExtentZ,
                            "§a" + selection.price() + " §b" + getModule().currencyString()
                        )
                    );
                } else {
                    boolean isPrimarySet = selection.primary != null;
                    boolean isSecondarySet = selection.secondary != null;
                    boolean sameWorld =
                        isPrimarySet &&
                        isSecondarySet &&
                        selection.primary.getWorld().equals(selection.secondary.getWorld());

                    boolean minimumSatisfied, maximumSatisfied, noIntersection, canAfford;
                    String sdx, sdy, sdz;
                    String price;
                    if (isPrimarySet && isSecondarySet && sameWorld) {
                        final var dx = 1 + Math.abs(selection.primary.getX() - selection.secondary.getX());
                        final var dy = 1 + Math.abs(selection.primary.getY() - selection.secondary.getY());
                        final var dz = 1 + Math.abs(selection.primary.getZ() - selection.secondary.getZ());
                        sdx = Integer.toString(dx);
                        sdy = Integer.toString(dy);
                        sdz = Integer.toString(dz);

                        minimumSatisfied =
                            dx >= getModule().configMinRegionExtentX &&
                            dy >= getModule().configMinRegionExtentY &&
                            dz >= getModule().configMinRegionExtentZ;
                        maximumSatisfied =
                            dx <= getModule().configMaxRegionExtentX &&
                            dy <= getModule().configMaxRegionExtentY &&
                            dz <= getModule().configMaxRegionExtentZ;
                        noIntersection = !selection.intersectsExisting();
                        canAfford = selection.canAfford(finalPlayer);
                        price = (canAfford ? "§a" : "§c") + selection.price() + " §b" + getModule().currencyString();
                    } else {
                        sdx = "§7?";
                        sdy = "§7?";
                        sdz = "§7?";
                        minimumSatisfied = false;
                        maximumSatisfied = false;
                        noIntersection = true;
                        canAfford = false;
                        price = "§7?";
                    }

                    final var extentColor = minimumSatisfied && maximumSatisfied ? "§a" : "§c";
                    super.item(
                        itemCreateRegionInvalidSelection.item(
                            isPrimarySet ? "§a✓" : "§c✕",
                            isSecondarySet ? "§a✓" : "§c✕",
                            sameWorld ? "§a✓" : "§c✕",
                            noIntersection ? "§a✓" : "§c✕",
                            minimumSatisfied ? "§a✓" : "§c✕",
                            maximumSatisfied ? "§a✓" : "§c✕",
                            canAfford ? "§a✓" : "§c✕",
                            extentColor + sdx,
                            extentColor + sdy,
                            extentColor + sdz,
                            "§b" + getModule().configMinRegionExtentX,
                            "§b" + getModule().configMinRegionExtentY,
                            "§b" + getModule().configMinRegionExtentZ,
                            "§b" + getModule().configMaxRegionExtentX,
                            "§b" + getModule().configMaxRegionExtentY,
                            "§b" + getModule().configMaxRegionExtentZ,
                            price
                        )
                    );
                }
            }
        };
    }

    private MenuWidget menuItemListRegions() {
        return new MenuItem(1, itemListRegions.item(), (player, menu, self) -> {
            menu.close(player);
            final var allRegions = getModule()
                .allRegions()
                .stream()
                .filter(r -> getModule().mayAdministrate(player, r))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .collect(Collectors.toList());

            final var filter = new Filter.StringFilter<Region>((r, str) -> r.name().toLowerCase().contains(str));
            MenuFactory.genericSelector(
                getContext(),
                player,
                langSelectRegionTitle.str(),
                langFilterRegionsTitle.str(),
                allRegions,
                r -> itemSelectRegion.item("§a§l" + r.name()),
                filter,
                (player2, m, region) -> {
                    m.close(player2);
                    getModule().menus.regionMenu.create(region, player2).open(player2);
                    return ClickResult.SUCCESS;
                },
                player2 -> menu.open(player2)
            ).open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemCurrentRegion(final Region region) {
        return new MenuItem(2, itemCurrentRegion.item("§a§l" + region.name()), (player, menu, self) -> {
            menu.close(player);
            getModule().menus.regionMenu.create(region, player).open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemCreateRegionGroup() {
        return new MenuItem(7, itemCreateRegionGroup.item(), (player, menu, self) -> {
            menu.close(player);
            getModule()
                .menus.enterRegionGroupNameMenu.create(player, (player2, name) -> {
                    final var group = new RegionGroup(name, player2.getUniqueId());
                    getModule().addRegionGroup(group);
                    getModule().menus.regionGroupMenu.create(group, player).open(player);
                    return ClickResult.SUCCESS;
                })
                .onNaturalClose(player2 -> menu.open(player2))
                .open(player);

            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemListRegionGroups() {
        return new MenuItem(8, itemListRegionGroups.item(), (player, menu, self) -> {
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
                    m.close(player2);
                    getModule().menus.regionGroupMenu.create(group, player).open(player);
                    return ClickResult.SUCCESS;
                },
                player2 -> menu.open(player2)
            ).open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemCurrentRegionGroup(final RegionGroup regionGroup) {
        return new MenuItem(6, itemCurrentRegionGroup.item("§a§l" + regionGroup.name()), (player, menu, self) -> {
            menu.close(player);
            getModule().menus.regionGroupMenu.create(regionGroup, player).open(player);
            return ClickResult.SUCCESS;
        });
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
