package org.oddlama.vane.regions.menu;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
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
import org.oddlama.vane.regions.region.EnvironmentSetting;
import org.oddlama.vane.regions.region.RegionGroup;
import org.oddlama.vane.regions.region.Role;
import org.oddlama.vane.util.StorageUtil;

public class RegionGroupMenu extends ModuleComponent<Regions> {

    @LangMessage
    public TranslatedMessage langTitle;

    @LangMessage
    public TranslatedMessage langDeleteConfirmTitle;

    @LangMessage
    public TranslatedMessage langSelectRoleTitle;

    @LangMessage
    public TranslatedMessage langFilterRolesTitle;

    public TranslatedItemStack<?> itemRename;
    public TranslatedItemStack<?> itemDelete;
    public TranslatedItemStack<?> itemDeleteConfirmAccept;
    public TranslatedItemStack<?> itemDeleteConfirmCancel;
    public TranslatedItemStack<?> itemCreateRole;
    public TranslatedItemStack<?> itemListRoles;
    public TranslatedItemStack<?> itemSelectRole;

    public TranslatedItemStack<?> itemSettingToggleOn;
    public TranslatedItemStack<?> itemSettingToggleOff;
    public TranslatedItemStack<?> itemSettingInfoAnimals;
    public TranslatedItemStack<?> itemSettingInfoMonsters;
    public TranslatedItemStack<?> itemSettingInfoExplosions;
    public TranslatedItemStack<?> itemSettingInfoFire;
    public TranslatedItemStack<?> itemSettingInfoPvp;
    public TranslatedItemStack<?> itemSettingInfoTrample;
    public TranslatedItemStack<?> itemSettingInfoVineGrowth;

    public RegionGroupMenu(Context<Regions> context) {
        super(context.namespace("RegionGroup"));
        final var ctx = getContext();
        itemRename = new TranslatedItemStack<>(
            ctx,
            "Rename",
            Material.NAME_TAG,
            1,
            "Used to rename the region group."
        );
        itemDelete = new TranslatedItemStack<>(
            ctx,
            "Delete",
            StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
            1,
            "Used to delete this region group."
        );
        itemDeleteConfirmAccept = new TranslatedItemStack<>(
            ctx,
            "DeleteConfirmAccept",
            StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
            1,
            "Used to confirm deleting the region group."
        );
        itemDeleteConfirmCancel = new TranslatedItemStack<>(
            ctx,
            "DeleteConfirmCancel",
            Material.PRISMARINE_SHARD,
            1,
            "Used to cancel deleting the region group."
        );
        itemCreateRole = new TranslatedItemStack<>(
            ctx,
            "CreateRole",
            Material.WRITABLE_BOOK,
            1,
            "Used to create a new role."
        );
        itemListRoles = new TranslatedItemStack<>(
            ctx,
            "ListRoles",
            Material.GLOBE_BANNER_PATTERN,
            1,
            "Used to list all defined roles."
        );
        itemSelectRole = new TranslatedItemStack<>(
            ctx,
            "SelectRole",
            Material.GLOBE_BANNER_PATTERN,
            1,
            "Used to represent a role in the role selection list."
        );

        itemSettingToggleOn = new TranslatedItemStack<>(
            ctx,
            "SettingToggleOn",
            Material.GREEN_TERRACOTTA,
            1,
            "Used to represent a toggle button with current state on."
        );
        itemSettingToggleOff = new TranslatedItemStack<>(
            ctx,
            "SettingToggleOff",
            Material.RED_TERRACOTTA,
            1,
            "Used to represent a toggle button with current state off."
        );
        itemSettingInfoAnimals = new TranslatedItemStack<>(
            ctx,
            "SettingInfoAnimals",
            StorageUtil.namespacedKey("vane", "animals_baby_pig_2"),
            1,
            "Used to represent the info for the animals setting."
        );
        itemSettingInfoMonsters = new TranslatedItemStack<>(
            ctx,
            "SettingInfoMonsters",
            Material.ZOMBIE_HEAD,
            1,
            "Used to represent the info for the monsters setting."
        );
        itemSettingInfoExplosions = new TranslatedItemStack<>(
            ctx,
            "SettingInfoExplosions",
            StorageUtil.namespacedKey("vane", "monsters_creeper_with_tnt_2"),
            1,
            "Used to represent the info for the explosions setting."
        );
        itemSettingInfoFire = new TranslatedItemStack<>(
            ctx,
            "SettingInfoFire",
            Material.CAMPFIRE,
            1,
            "Used to represent the info for the fire setting."
        );
        itemSettingInfoPvp = new TranslatedItemStack<>(
            ctx,
            "SettingInfoPvP",
            Material.IRON_SWORD,
            1,
            "Used to represent the info for the pvp setting."
        );
        itemSettingInfoTrample = new TranslatedItemStack<>(
            ctx,
            "SettingInfoTrample",
            Material.FARMLAND,
            1,
            "Used to represent the info for the trample setting."
        );
        itemSettingInfoVineGrowth = new TranslatedItemStack<>(
            ctx,
            "SettingInfoVineGrowth",
            Material.VINE,
            1,
            "Used to represent the info for the vine growth setting."
        );
    }

    public Menu create(final RegionGroup group, final Player player) {
        final var columns = 9;
        final var rows = 3;
        final var title = langTitle.strComponent("§5§l" + group.name());
        final var regionGroupMenu = new Menu(getContext(), Bukkit.createInventory(null, rows * columns, title));
        regionGroupMenu.tag(new RegionGroupMenuTag(group.id()));

        final var isOwner = player.getUniqueId().equals(group.owner());
        if (isOwner) {
            regionGroupMenu.add(menuItemRename(group));
            // Delete it only if this isn't the default group
            if (!getModule().getOrCreateDefaultRegionGroup(player).id().equals(group.id())) {
                regionGroupMenu.add(menuItemDelete(group));
            }
        }

        regionGroupMenu.add(menuItemCreateRole(group));
        regionGroupMenu.add(menuItemListRoles(group));

        addMenuItemSetting(regionGroupMenu, group, 0, itemSettingInfoAnimals, EnvironmentSetting.ANIMALS);
        addMenuItemSetting(regionGroupMenu, group, 1, itemSettingInfoMonsters, EnvironmentSetting.MONSTERS);
        addMenuItemSetting(regionGroupMenu, group, 3, itemSettingInfoExplosions, EnvironmentSetting.EXPLOSIONS);
        addMenuItemSetting(regionGroupMenu, group, 4, itemSettingInfoFire, EnvironmentSetting.FIRE);
        addMenuItemSetting(regionGroupMenu, group, 5, itemSettingInfoPvp, EnvironmentSetting.PVP);
        addMenuItemSetting(regionGroupMenu, group, 7, itemSettingInfoTrample, EnvironmentSetting.TRAMPLE);
        addMenuItemSetting(
            regionGroupMenu,
            group,
            8,
                itemSettingInfoVineGrowth,
            EnvironmentSetting.VINE_GROWTH
        );

        regionGroupMenu.onNaturalClose(player2 -> getModule().menus.mainMenu.create(player2).open(player2));

        return regionGroupMenu;
    }

    private MenuWidget menuItemRename(final RegionGroup group) {
        return new MenuItem(0, itemRename.item(), (player, menu, self) -> {
            menu.close(player);

            getModule()
                .menus.enterRegionGroupNameMenu.create(player, group.name(), (player2, name) -> {
                    group.name(name);
                    markPersistentStorageDirty();

                    // Open new menu because of possibly changed title
                    getModule().menus.regionGroupMenu.create(group, player2).open(player2);
                    return ClickResult.SUCCESS;
                })
                .onNaturalClose(player2 -> {
                    // Open new menu because of possibly changed title
                    getModule().menus.regionGroupMenu.create(group, player2).open(player2);
                })
                .open(player);

            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemDelete(final RegionGroup group) {
        final var orphanCheckbox = group.isOrphan(getModule()) ? "§a✓" : "§c✕";
        return new MenuItem(1, itemDelete.item(orphanCheckbox), (player, menu, self) -> {
            if (!group.isOrphan(getModule())) {
                return ClickResult.ERROR;
            }

            menu.close(player);
            MenuFactory.confirm(
                getContext(),
                langDeleteConfirmTitle.str(),
                itemDeleteConfirmAccept.item(),
                player2 -> {
                    if (!player2.getUniqueId().equals(group.owner())) {
                        return ClickResult.ERROR;
                    }

                    // Assert that this isn't the default group
                    if (getModule().getOrCreateDefaultRegionGroup(player2).id().equals(group.id())) {
                        return ClickResult.ERROR;
                    }

                    getModule().removeRegionGroup(group);
                    return ClickResult.SUCCESS;
                },
                itemDeleteConfirmCancel.item(),
                player2 -> menu.open(player2)
            )
                .tag(new RegionGroupMenuTag(group.id()))
                .open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemCreateRole(final RegionGroup group) {
        return new MenuItem(7, itemCreateRole.item(), (player, menu, self) -> {
            menu.close(player);
            getModule()
                .menus.enterRoleNameMenu.create(player, (player2, name) -> {
                    final var role = new Role(name, Role.RoleType.NORMAL);
                    group.addRole(role);
                    markPersistentStorageDirty();
                    getModule().menus.roleMenu.create(group, role, player).open(player);
                    return ClickResult.SUCCESS;
                })
                .onNaturalClose(player2 -> menu.open(player2))
                .open(player);

            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemListRoles(final RegionGroup group) {
        return new MenuItem(8, itemListRoles.item(), (player, menu, self) -> {
            menu.close(player);
            final var allRoles = group
                .roles()
                .stream()
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .collect(Collectors.toList());

            final var filter = new Filter.StringFilter<Role>((r, str) -> r.name().toLowerCase().contains(str));
            MenuFactory.genericSelector(
                getContext(),
                player,
                langSelectRoleTitle.str(),
                langFilterRolesTitle.str(),
                allRoles,
                r -> itemSelectRole.item(r.color() + "§l" + r.name()),
                filter,
                (player2, m, role) -> {
                    m.close(player2);
                    getModule().menus.roleMenu.create(group, role, player2).open(player2);
                    return ClickResult.SUCCESS;
                },
                player2 -> menu.open(player2)
            ).open(player);
            return ClickResult.SUCCESS;
        });
    }

    private void addMenuItemSetting(
        final Menu regionGroupMenu,
        final RegionGroup group,
        final int col,
        final TranslatedItemStack<?> itemInfo,
        final EnvironmentSetting setting
    ) {
        regionGroupMenu.add(new MenuItem(9 + col, itemInfo.item(), (player, menu, self) -> ClickResult.IGNORE));

        regionGroupMenu.add(
            new MenuItem(2 * 9 + col, null, (player, menu, self) -> {
                // Prevent toggling when the server forces the setting
                if (setting.hasOverride()) {
                    return ClickResult.ERROR;
                }

                group.settings().put(setting, !group.getSetting(setting));
                markPersistentStorageDirty();
                menu.update();
                return ClickResult.SUCCESS;
            }) {
                @Override
                public void item(final ItemStack item) {
                    final Consumer<List<Component>> maybeAddForcedHint = lore -> {
                        if (setting.hasOverride()) {
                            lore.add(Component.empty());
                            lore.add(Component.text("FORCED BY SERVER"));
                        }
                    };

                    if (group.getSetting(setting)) {
                        super.item(itemSettingToggleOn.itemTransformLore(maybeAddForcedHint));
                    } else {
                        super.item(itemSettingToggleOff.itemTransformLore(maybeAddForcedHint));
                    }
                }
            }
        );
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
