package org.oddlama.vane.portals.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Menu;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.menu.MenuItem;
import org.oddlama.vane.core.menu.MenuWidget;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.Portals;
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent;
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.util.StorageUtil;

public class SettingsMenu extends ModuleComponent<Portals> {

    @LangMessage
    public TranslatedMessage langTitle;

    @LangMessage
    public TranslatedMessage langSelectIconTitle;

    public TranslatedItemStack<?> itemRename;
    public TranslatedItemStack<?> itemSelectIcon;
    public TranslatedItemStack<?> itemSelectStyle;
    public TranslatedItemStack<?> itemExitOrientationLockOn;
    public TranslatedItemStack<?> itemExitOrientationLockOff;
    public TranslatedItemStack<?> itemVisibilityPublic;
    public TranslatedItemStack<?> itemVisibilityGroup;
    public TranslatedItemStack<?> itemVisibilityGroupInternal;
    public TranslatedItemStack<?> itemVisibilityPrivate;
    public TranslatedItemStack<?> itemTargetLockOn;
    public TranslatedItemStack<?> itemTargetLockOff;
    public TranslatedItemStack<?> itemBack;

    public SettingsMenu(Context<Portals> context) {
        super(context.namespace("Settings"));
        final var ctx = getContext();
        itemRename = new TranslatedItemStack<>(ctx, "Rename", Material.NAME_TAG, 1, "Used to rename the portal.");
        itemSelectIcon = new TranslatedItemStack<>(
            ctx,
            "SelectIcon",
            StorageUtil.namespacedKey("vane", "decoration_end_portal_orb"),
            1,
            "Used to select the portal's icon."
        );
        itemSelectStyle = new TranslatedItemStack<>(
            ctx,
            "SelectStyle",
            Material.ITEM_FRAME,
            1,
            "Used to change the portal's style."
        );
        itemExitOrientationLockOn = new TranslatedItemStack<>(
            ctx,
            "ExitOrientationLockOn",
            Material.SOUL_TORCH,
            1,
            "Used to toggle and indicate enabled exit orientation lock."
        );
        itemExitOrientationLockOff = new TranslatedItemStack<>(
            ctx,
            "ExitOrientationLockOff",
            Material.TORCH,
            1,
            "Used to toggle and indicate disabled exit orientation lock."
        );
        itemVisibilityPublic = new TranslatedItemStack<>(
            ctx,
            "VisibilityPublic",
            Material.ENDER_EYE,
            1,
            "Used to change and indicate public visibility."
        );
        itemVisibilityGroup = new TranslatedItemStack<>(
            ctx,
            "VisibilityGroup",
            Material.ENDER_PEARL,
            1,
            "Used to change and indicate group visibility."
        );
        itemVisibilityGroupInternal = new TranslatedItemStack<>(
            ctx,
            "VisibilityGroupInternal",
            Material.FIRE_CHARGE,
            1,
            "Used to change and indicate group internal visibility."
        );
        itemVisibilityPrivate = new TranslatedItemStack<>(
            ctx,
            "VisibilityPrivate",
            Material.FIREWORK_STAR,
            1,
            "Used to change and indicate private visibility."
        );
        itemTargetLockOn = new TranslatedItemStack<>(
            ctx,
            "TargetLockOn",
            Material.SLIME_BALL,
            1,
            "Used to toggle and indicate enabled target lock."
        );
        itemTargetLockOff = new TranslatedItemStack<>(
            ctx,
            "TargetLockOff",
            Material.SNOWBALL,
            1,
            "Used to toggle and indicate disabled target lock."
        );
        itemBack = new TranslatedItemStack<>(
            ctx,
            "Back",
            Material.PRISMARINE_SHARD,
            1,
            "Used to go back to the previous menu."
        );
    }

    // HINT: We don't capture the previous menu and open a new one on exit,
    // to correctly reflect changes done in here. (e.g., menu title due to portal name)
    public Menu create(final Portal portal, final Player player, final Block console) {
        final var columns = 9;
        final var title = langTitle.strComponent("§5§l" + portal.name());
        final var settingsMenu = new Menu(getContext(), Bukkit.createInventory(null, columns, title));
        settingsMenu.tag(new PortalMenuTag(portal.id()));

        settingsMenu.add(menuItemRename(portal, console));
        settingsMenu.add(menuItemSelectIcon(portal));
        settingsMenu.add(menuItemSelectStyle(portal));
        settingsMenu.add(menuItemExitOrientationLock(portal));
        settingsMenu.add(menuItemVisibility(portal));
        settingsMenu.add(menuItemTargetLock(portal));
        settingsMenu.add(menuItemBack(portal, console));

        settingsMenu.onNaturalClose(player2 ->
            getModule().menus.consoleMenu.create(portal, player2, console).open(player2)
        );

        return settingsMenu;
    }

    private MenuWidget menuItemRename(final Portal portal, final Block console) {
        return new MenuItem(0, itemRename.item(), (player, menu, self) -> {
            menu.close(player);

            getModule()
                .menus.enterNameMenu.create(player, portal.name(), (player2, name) -> {
                    final var settingsEvent = new PortalChangeSettingsEvent(player2, portal, false);
                    getModule().getServer().getPluginManager().callEvent(settingsEvent);
                    if (settingsEvent.isCancelled() && !player2.hasPermission(getModule().adminPermission)) {
                        getModule().langSettingsRestricted.send(player2);
                        return ClickResult.ERROR;
                    }

                    portal.name(name);

                    // Update portal icons to reflect new name
                    getModule().updatePortalIcon(portal);

                    // Open new menu because of possibly changed title
                    getModule().menus.settingsMenu.create(portal, player2, console).open(player2);
                    return ClickResult.SUCCESS;
                })
                .onNaturalClose(player2 -> {
                    // Open new menu because of possibly changed title
                    getModule().menus.settingsMenu.create(portal, player2, console).open(player2);
                })
                .open(player);

            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemSelectIcon(final Portal portal) {
        return new MenuItem(1, itemSelectIcon.item(), (player, menu, self) -> {
            menu.close(player);
            MenuFactory.itemSelector(
                getContext(),
                player,
                langSelectIconTitle.str(),
                portal.icon(),
                true,
                (player2, item) -> {
                    final var settingsEvent = new PortalChangeSettingsEvent(player2, portal, false);
                    getModule().getServer().getPluginManager().callEvent(settingsEvent);
                    if (settingsEvent.isCancelled() && !player2.hasPermission(getModule().adminPermission)) {
                        getModule().langSettingsRestricted.send(player2);
                        return ClickResult.ERROR;
                    }

                    portal.icon(item);
                    getModule().updatePortalIcon(portal);
                    menu.open(player2);
                    return ClickResult.SUCCESS;
                },
                player2 -> menu.open(player2)
            )
                .tag(new PortalMenuTag(portal.id()))
                .open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemSelectStyle(final Portal portal) {
        return new MenuItem(2, itemSelectStyle.item(), (player, menu, self) -> {
            final var settingsEvent = new PortalChangeSettingsEvent(player, portal, false);
            getModule().getServer().getPluginManager().callEvent(settingsEvent);
            if (settingsEvent.isCancelled() && !player.hasPermission(getModule().adminPermission)) {
                getModule().langSettingsRestricted.send(player);
                return ClickResult.ERROR;
            }

            menu.close(player);
            getModule().menus.styleMenu.create(portal, player, menu).open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemExitOrientationLock(final Portal portal) {
        return new MenuItem(4, null, (player, menu, self) -> {
            final var settingsEvent = new PortalChangeSettingsEvent(player, portal, false);
            getModule().getServer().getPluginManager().callEvent(settingsEvent);
            if (settingsEvent.isCancelled() && !player.hasPermission(getModule().adminPermission)) {
                getModule().langSettingsRestricted.send(player);
                return ClickResult.ERROR;
            }

            portal.exitOrientationLocked(!portal.exitOrientationLocked());
            menu.update();
            return ClickResult.SUCCESS;
        }) {
            @Override
            public void item(final ItemStack item) {
                if (portal.exitOrientationLocked()) {
                    super.item(itemExitOrientationLockOn.item());
                } else {
                    super.item(itemExitOrientationLockOff.item());
                }
            }
        };
    }

    private MenuWidget menuItemVisibility(final Portal portal) {
        return new MenuItem(5, null, (player, menu, self, event) -> {
            if (!Menu.isLeftOrRightClick(event)) {
                return ClickResult.INVALID_CLICK;
            }

            final var settingsEvent = new PortalChangeSettingsEvent(player, portal, false);
            getModule().getServer().getPluginManager().callEvent(settingsEvent);
            if (settingsEvent.isCancelled() && !player.hasPermission(getModule().adminPermission)) {
                getModule().langSettingsRestricted.send(player);
                return ClickResult.ERROR;
            }

            Portal.Visibility newVis = portal.visibility();
            // If the "regions" plugin is not installed, we need to skip group visibility.
            do {
                newVis = event.getClick() == ClickType.RIGHT ? newVis.prev() : newVis.next();
            } while (newVis.requiresRegions() && !getModule().isRegionsInstalled());

            portal.visibility(newVis);
            getModule().updatePortalVisibility(portal);
            menu.update();
            return ClickResult.SUCCESS;
        }) {
            @Override
            public void item(final ItemStack item) {
                switch (portal.visibility()) {
                    case PUBLIC:
                        super.item(itemVisibilityPublic.item());
                        break;
                    case GROUP:
                        super.item(itemVisibilityGroup.item());
                        break;
                    case GROUP_INTERNAL:
                        super.item(itemVisibilityGroupInternal.item());
                        break;
                    case PRIVATE:
                        super.item(itemVisibilityPrivate.item());
                        break;
                }
            }
        };
    }

    private MenuWidget menuItemTargetLock(final Portal portal) {
        return new MenuItem(6, null, (player, menu, self) -> {
            final var settingsEvent = new PortalChangeSettingsEvent(player, portal, false);
            getModule().getServer().getPluginManager().callEvent(settingsEvent);
            if (settingsEvent.isCancelled() && !player.hasPermission(getModule().adminPermission)) {
                getModule().langSettingsRestricted.send(player);
                return ClickResult.ERROR;
            }

            portal.targetLocked(!portal.targetLocked());
            menu.update();
            return ClickResult.SUCCESS;
        }) {
            @Override
            public void item(final ItemStack item) {
                if (portal.targetLocked()) {
                    super.item(itemTargetLockOn.item());
                } else {
                    super.item(itemTargetLockOff.item());
                }
            }
        };
    }

    private MenuWidget menuItemBack(final Portal portal, final Block console) {
        return new MenuItem(8, itemBack.item(), (player, menu, self) -> {
            menu.close(player);
            getModule().menus.consoleMenu.create(portal, player, console).open(player);
            return ClickResult.SUCCESS;
        });
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
