package org.oddlama.vane.regions.menu;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import org.oddlama.vane.regions.region.RegionGroup;
import org.oddlama.vane.regions.region.Role;
import org.oddlama.vane.regions.region.RoleSetting;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.StorageUtil;

public class RoleMenu extends ModuleComponent<Regions> {

    @LangMessage
    public TranslatedMessage langTitle;

    @LangMessage
    public TranslatedMessage langDeleteConfirmTitle;

    @LangMessage
    public TranslatedMessage langSelectAssignPlayerTitle;

    @LangMessage
    public TranslatedMessage langSelectRemovePlayerTitle;

    @LangMessage
    public TranslatedMessage langFilterPlayersTitle;

    public TranslatedItemStack<?> itemRename;
    public TranslatedItemStack<?> itemDelete;
    public TranslatedItemStack<?> itemDeleteConfirmAccept;
    public TranslatedItemStack<?> itemDeleteConfirmCancel;
    public TranslatedItemStack<?> itemAssignPlayer;
    public TranslatedItemStack<?> itemRemovePlayer;
    public TranslatedItemStack<?> itemSelectPlayer;

    public TranslatedItemStack<?> itemSettingToggleOn;
    public TranslatedItemStack<?> itemSettingToggleOff;
    public TranslatedItemStack<?> itemSettingInfoAdmin;
    public TranslatedItemStack<?> itemSettingInfoBuild;
    public TranslatedItemStack<?> itemSettingInfoUse;
    public TranslatedItemStack<?> itemSettingInfoContainer;
    public TranslatedItemStack<?> itemSettingInfoPortal;

    public RoleMenu(Context<Regions> context) {
        super(context.namespace("Role"));
        final var ctx = getContext();
        itemRename = new TranslatedItemStack<>(ctx, "Rename", Material.NAME_TAG, 1, "Used to rename the role.");
        itemDelete = new TranslatedItemStack<>(
            ctx,
            "Delete",
            StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
            1,
            "Used to delete this role."
        );
        itemDeleteConfirmAccept = new TranslatedItemStack<>(
            ctx,
            "DeleteConfirmAccept",
            StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
            1,
            "Used to confirm deleting the role."
        );
        itemDeleteConfirmCancel = new TranslatedItemStack<>(
            ctx,
            "DeleteConfirmCancel",
            Material.PRISMARINE_SHARD,
            1,
            "Used to cancel deleting the role."
        );
        itemAssignPlayer = new TranslatedItemStack<>(
            ctx,
            "AssignPlayer",
            Material.PLAYER_HEAD,
            1,
            "Used to assign players to this role."
        );
        itemRemovePlayer = new TranslatedItemStack<>(
            ctx,
            "RemovePlayer",
            Material.PLAYER_HEAD,
            1,
            "Used to remove players from this role."
        );
        itemSelectPlayer = new TranslatedItemStack<>(
            ctx,
            "SelectPlayer",
            Material.PLAYER_HEAD,
            1,
            "Used to represent a player in the role assignment/removal list."
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
        itemSettingInfoAdmin = new TranslatedItemStack<>(
            ctx,
            "SettingInfoAdmin",
            Material.GOLDEN_APPLE,
            1,
            "Used to represent the info for the admin setting."
        );
        itemSettingInfoBuild = new TranslatedItemStack<>(
            ctx,
            "SettingInfoBuild",
            Material.IRON_PICKAXE,
            1,
            "Used to represent the info for the build setting."
        );
        itemSettingInfoUse = new TranslatedItemStack<>(
            ctx,
            "SettingInfoUse",
            Material.DARK_OAK_DOOR,
            1,
            "Used to represent the info for the use setting."
        );
        itemSettingInfoContainer = new TranslatedItemStack<>(
            ctx,
            "SettingInfoContainer",
            Material.CHEST,
            1,
            "Used to represent the info for the container setting."
        );

        itemSettingInfoPortal = new TranslatedItemStack<>(
            ctx,
            "SettingInfoPortal",
            Material.ENDER_PEARL,
            1,
            "Used to represent the info for the portal setting."
        );
    }

    public Menu create(final RegionGroup group, final Role role, final Player player) {
        final var columns = 9;
        final var rows = 3;
        final var title = langTitle.strComponent(role.color() + "§l" + role.name());
        final var roleMenu = new Menu(getContext(), Bukkit.createInventory(null, rows * columns, title));

        final var isAdmin =
            player.getUniqueId().equals(group.owner()) ||
            group.getRole(player.getUniqueId()).getSetting(RoleSetting.ADMIN);

        if (isAdmin && role.roleType() == Role.RoleType.NORMAL) {
            roleMenu.add(menuItemRename(group, role));
            roleMenu.add(menuItemDelete(group, role));
        }

        if (role.roleType() != Role.RoleType.OTHERS) {
            roleMenu.add(menuItemAssignPlayer(group, role));
            roleMenu.add(menuItemRemovePlayer(group, role));
        }

        addMenuItemSetting(roleMenu, role, 0, itemSettingInfoAdmin, RoleSetting.ADMIN);
        addMenuItemSetting(roleMenu, role, 2, itemSettingInfoBuild, RoleSetting.BUILD);
        addMenuItemSetting(roleMenu, role, 4, itemSettingInfoUse, RoleSetting.USE);
        addMenuItemSetting(roleMenu, role, 5, itemSettingInfoContainer, RoleSetting.CONTAINER);

        if (getModule().vanePortalsAvailable) {
            addMenuItemSetting(roleMenu, role, 8, itemSettingInfoPortal, RoleSetting.PORTAL);
        }

        roleMenu.onNaturalClose(player2 -> getModule().menus.regionGroupMenu.create(group, player2).open(player2)
        );

        return roleMenu;
    }

    private MenuWidget menuItemRename(final RegionGroup group, final Role role) {
        return new MenuItem(0, itemRename.item(), (player, menu, self) -> {
            menu.close(player);

            getModule()
                .menus.enterRoleNameMenu.create(player, role.name(), (player2, name) -> {
                    role.name(name);
                    markPersistentStorageDirty();

                    // Open new menu because of possibly changed title
                    getModule().menus.roleMenu.create(group, role, player2).open(player2);
                    return ClickResult.SUCCESS;
                })
                .onNaturalClose(player2 -> {
                    // Open new menu because of possibly changed title
                    getModule().menus.roleMenu.create(group, role, player2).open(player2);
                })
                .open(player);

            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemDelete(final RegionGroup group, final Role role) {
        return new MenuItem(1, itemDelete.item(), (player, menu, self) -> {
            menu.close(player);
            MenuFactory.confirm(
                getContext(),
                langDeleteConfirmTitle.str(),
                itemDeleteConfirmAccept.item(),
                player2 -> {
                    group.removeRole(role.id());
                    markPersistentStorageDirty();
                    return ClickResult.SUCCESS;
                },
                itemDeleteConfirmCancel.item(),
                player2 -> menu.open(player2)
            ).open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemAssignPlayer(final RegionGroup group, final Role role) {
        return new MenuItem(7, itemAssignPlayer.item(), (player, menu, self) -> {
            menu.close(player);
            final var allPlayers = getModule()
                .getOfflinePlayersWithValidName()
                .stream()
                .filter(p -> !role.id().equals(group.playerToRole().get(p.getUniqueId())))
                .sorted((a, b) -> {
                    int c = Boolean.compare(b.isOnline(), a.isOnline());
                    if (c != 0) {
                        return c;
                    }
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .collect(Collectors.toList());

            final var filter = new Filter.StringFilter<OfflinePlayer>((p, str) ->
                p.getName().toLowerCase().contains(str)
            );
            MenuFactory.genericSelector(
                getContext(),
                player,
                langSelectAssignPlayerTitle.str(),
                langFilterPlayersTitle.str(),
                allPlayers,
                p -> itemSelectPlayer.alternative(ItemUtil.skullForPlayer(p, true), "§a§l" + p.getName()),
                filter,
                (player2, m, p) -> {
                    allPlayers.remove(p);
                    m.update();
                    group.playerToRole().put(p.getUniqueId(), role.id());
                    return ClickResult.SUCCESS;
                },
                player2 -> menu.open(player2)
            ).open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemRemovePlayer(final RegionGroup group, final Role role) {
        return new MenuItem(8, itemRemovePlayer.item(), (player, menu, self) -> {
            menu.close(player);
            final var allPlayers = getModule()
                .getOfflinePlayersWithValidName()
                .stream()
                .filter(p -> role.id().equals(group.playerToRole().get(p.getUniqueId())))
                .sorted((a, b) -> {
                    int c = Boolean.compare(b.isOnline(), a.isOnline());
                    if (c != 0) {
                        return c;
                    }
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .collect(Collectors.toList());

            final var filter = new Filter.StringFilter<OfflinePlayer>((p, str) ->
                p.getName().toLowerCase().contains(str)
            );
            MenuFactory.genericSelector(
                getContext(),
                player,
                langSelectRemovePlayerTitle.str(),
                langFilterPlayersTitle.str(),
                allPlayers,
                p -> itemSelectPlayer.alternative(ItemUtil.skullForPlayer(p, true), "§a§l" + p.getName()),
                filter,
                (player2, m, p) -> {
                    allPlayers.remove(p);
                    m.update();
                    group.playerToRole().remove(p.getUniqueId());
                    return ClickResult.SUCCESS;
                },
                player2 -> menu.open(player2)
            ).open(player);
            return ClickResult.SUCCESS;
        });
    }

    private void addMenuItemSetting(
        final Menu roleMenu,
        final Role role,
        final int col,
        final TranslatedItemStack<?> itemInfo,
        final RoleSetting setting
    ) {
        roleMenu.add(new MenuItem(9 + col, itemInfo.item(), (player, menu, self) -> ClickResult.IGNORE));

        roleMenu.add(
            new MenuItem(2 * 9 + col, null, (player, menu, self) -> {
                // Prevent toggling when the server forces the setting
                if (setting.hasOverride()) {
                    return ClickResult.ERROR;
                }

                if (setting == RoleSetting.ADMIN) {
                    // Admin setting is immutable
                    return ClickResult.ERROR;
                }

                role.settings().put(setting, !role.getSetting(setting));
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

                    if (role.getSetting(setting)) {
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
