package org.oddlama.vane.portals.menu;

import java.util.Objects;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
import org.oddlama.vane.portals.Portals;
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent;
import org.oddlama.vane.portals.event.PortalDestroyEvent;
import org.oddlama.vane.portals.event.PortalSelectTargetEvent;
import org.oddlama.vane.portals.event.PortalUnlinkConsoleEvent;
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.util.StorageUtil;

public class ConsoleMenu extends ModuleComponent<Portals> {

    @LangMessage
    public TranslatedMessage langTitle;

    @LangMessage
    public TranslatedMessage langUnlinkConsoleConfirmTitle;

    @LangMessage
    public TranslatedMessage langDestroyPortalConfirmTitle;

    @LangMessage
    public TranslatedMessage langSelectTargetTitle;

    @LangMessage
    public TranslatedMessage langFilterPortalsTitle;

    @LangMessage
    public TranslatedMessage langSelectTargetPortalVisibilityPublic;

    @LangMessage
    public TranslatedMessage langSelectTargetPortalVisibilityPrivate;

    @LangMessage
    public TranslatedMessage langSelectTargetPortalVisibilityGroup;

    @LangMessage
    public TranslatedMessage langSelectTargetPortalVisibilityGroupInternal;

    public TranslatedItemStack<?> itemSettings;
    public TranslatedItemStack<?> itemSelectTarget;
    public TranslatedItemStack<?> itemSelectTargetPortal;
    public TranslatedItemStack<?> itemSelectTargetLocked;
    public TranslatedItemStack<?> itemUnlinkConsole;
    public TranslatedItemStack<?> itemUnlinkConsoleConfirmAccept;
    public TranslatedItemStack<?> itemUnlinkConsoleConfirmCancel;
    public TranslatedItemStack<?> itemDestroyPortal;
    public TranslatedItemStack<?> itemDestroyPortalConfirmAccept;
    public TranslatedItemStack<?> itemDestroyPortalConfirmCancel;

    public ConsoleMenu(Context<Portals> context) {
        super(context.namespace("Console"));
        final var ctx = getContext();
        itemSettings = new TranslatedItemStack<>(
            ctx,
            "Settings",
            Material.WRITABLE_BOOK,
            1,
            "Used to enter portal settings."
        );
        itemSelectTarget = new TranslatedItemStack<>(
            ctx,
            "SelectTarget",
            Material.COMPASS,
            1,
            "Used to enter portal target selection."
        );
        itemSelectTargetPortal = new TranslatedItemStack<>(
            ctx,
            "SelectTargetPortal",
            Material.COMPASS,
            1,
            "Used to represent a portal in the target selection menu."
        );
        itemSelectTargetLocked = new TranslatedItemStack<>(
            ctx,
            "SelectTargetLocked",
            Material.FIREWORK_STAR,
            1,
            "Used to show portal target selection when the target is locked."
        );
        itemUnlinkConsole = new TranslatedItemStack<>(
            ctx,
            "UnlinkConsole",
            StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
            1,
            "Used to unlink the current console."
        );
        itemUnlinkConsoleConfirmAccept = new TranslatedItemStack<>(
            ctx,
            "UnlinkConsoleConfirmAccept",
            StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
            1,
            "Used to confirm unlinking the current console."
        );
        itemUnlinkConsoleConfirmCancel = new TranslatedItemStack<>(
            ctx,
            "UnlinkConsoleConfirmCancel",
            Material.PRISMARINE_SHARD,
            1,
            "Used to cancel unlinking the current console."
        );
        itemDestroyPortal = new TranslatedItemStack<>(
            ctx,
            "DestroyPortal",
            Material.TNT,
            1,
            "Used to destroy the portal."
        );
        itemDestroyPortalConfirmAccept = new TranslatedItemStack<>(
            ctx,
            "DestroyPortalConfirmAccept",
            Material.TNT,
            1,
            "Used to confirm destroying the portal."
        );
        itemDestroyPortalConfirmCancel = new TranslatedItemStack<>(
            ctx,
            "DestroyPortalConfirmCancel",
            Material.PRISMARINE_SHARD,
            1,
            "Used to cancel destroying the portal."
        );
    }

    public Menu create(final Portal portal, final Player player, final Block console) {
        final var columns = 9;
        final var title = langTitle.strComponent("§5§l" + portal.name());
        final var consoleMenu = new Menu(getContext(), Bukkit.createInventory(null, columns, title));
        consoleMenu.tag(new PortalMenuTag(portal.id()));

        // Check if target selection would be allowed
        final var selectTargetEvent = new PortalSelectTargetEvent(player, portal, null, true);
        getModule().getServer().getPluginManager().callEvent(selectTargetEvent);
        if (!selectTargetEvent.isCancelled() || player.hasPermission(getModule().adminPermission)) {
            consoleMenu.add(menuItemSelectTarget(portal));
        }

        // Check if settings would be allowed
        final var settingsEvent = new PortalChangeSettingsEvent(player, portal, true);
        getModule().getServer().getPluginManager().callEvent(settingsEvent);
        if (!settingsEvent.isCancelled() || player.hasPermission(getModule().adminPermission)) {
            consoleMenu.add(menuItemSettings(portal, console));
        }

        // Check if unlink would be allowed
        final var unlinkEvent = new PortalUnlinkConsoleEvent(player, console, portal, true);
        getModule().getServer().getPluginManager().callEvent(unlinkEvent);
        if (!unlinkEvent.isCancelled() || player.hasPermission(getModule().adminPermission)) {
            consoleMenu.add(menuItemUnlinkConsole(portal, console));
        }

        // Check if destroy would be allowed
        final var destroyEvent = new PortalDestroyEvent(player, portal, true);
        getModule().getServer().getPluginManager().callEvent(destroyEvent);
        if (!destroyEvent.isCancelled() || player.hasPermission(getModule().adminPermission)) {
            consoleMenu.add(menuItemDestroyPortal(portal));
        }

        return consoleMenu;
    }

    private MenuWidget menuItemSettings(final Portal portal, final Block console) {
        return new MenuItem(0, itemSettings.item(), (player, menu, self) -> {
            final var settingsEvent = new PortalChangeSettingsEvent(player, portal, false);
            getModule().getServer().getPluginManager().callEvent(settingsEvent);
            if (settingsEvent.isCancelled() && !player.hasPermission(getModule().adminPermission)) {
                getModule().langSettingsRestricted.send(player);
                return ClickResult.ERROR;
            }

            menu.close(player);
            getModule().menus.settingsMenu.create(portal, player, console).open(player);
            return ClickResult.SUCCESS;
        });
    }

    private Component portalVisibility(Portal.Visibility visibility) {
        return (
            switch (visibility) {
                case PUBLIC -> langSelectTargetPortalVisibilityPublic;
                case GROUP -> langSelectTargetPortalVisibilityGroup;
                case GROUP_INTERNAL -> langSelectTargetPortalVisibilityGroupInternal;
                case PRIVATE -> langSelectTargetPortalVisibilityPrivate;
            }
        ).format();
    }

    private MenuWidget menuItemSelectTarget(final Portal portal) {
        return new MenuItem(4, null, (player, menu, self) -> {
            if (portal.targetLocked()) {
                return ClickResult.ERROR;
            } else {
                menu.close(player);
                final var allPortals = getModule()
                    .allAvailablePortals()
                    .stream()
                    .filter(p -> {
                        switch (p.visibility()) {
                            case PUBLIC:
                                return true;
                            case GROUP:
                                return getModule().playerCanUsePortalsInRegionGroupOf(player, p);
                            case GROUP_INTERNAL:
                                return getModule().isInSameRegionGroup(portal, p);
                            case PRIVATE:
                                return player.getUniqueId().equals(p.owner());
                        }
                        return false;
                    })
                    .filter(p -> !Objects.equals(p.id(), portal.id()))
                    .sorted(new Portal.TargetSelectionComparator(player))
                    .collect(Collectors.toList());

                final var filter = new Filter.StringFilter<Portal>((p, str) -> p.name().toLowerCase().contains(str));
                MenuFactory.genericSelector(
                    getContext(),
                    player,
                    langSelectTargetTitle.str(),
                    langFilterPortalsTitle.str(),
                    allPortals,
                    p -> {
                        final var dist = p
                            .spawn()
                            .toVector()
                            .setY(0.0)
                            .distance(player.getLocation().toVector().setY(0.0));
                        return itemSelectTargetPortal.alternative(
                            getModule().iconFor(p),
                            "§a§l" + p.name(),
                            "§6" + String.format("%.1f", dist),
                            "§b" + p.spawn().getWorld().getName(),
                            portalVisibility(p.visibility())
                        );
                    },
                    filter,
                    (player2, m, t) -> {
                        m.close(player2);

                        final var selectTargetEvent = new PortalSelectTargetEvent(player2, portal, t, false);
                        getModule().getServer().getPluginManager().callEvent(selectTargetEvent);
                        if (
                            selectTargetEvent.isCancelled() && !player2.hasPermission(getModule().adminPermission)
                        ) {
                            getModule().langSelectTargetRestricted.send(player2);
                            return ClickResult.ERROR;
                        }

                        portal.targetId(t.id());

                        // Update portal block to reflect new target on consoles
                        portal.updateBlocks(getModule());
                        return ClickResult.SUCCESS;
                    },
                    player2 -> menu.open(player2)
                )
                    .tag(new PortalMenuTag(portal.id()))
                    .open(player);
                return ClickResult.SUCCESS;
            }
        }) {
            @Override
            public void item(final ItemStack item) {
                final var target = portal.target(getModule());
                final var targetName = "§a" + (target == null ? "None" : target.name());
                if (portal.targetLocked()) {
                    super.item(itemSelectTargetLocked.item(targetName));
                } else {
                    super.item(itemSelectTarget.item(targetName));
                }
            }
        };
    }

    private MenuWidget menuItemUnlinkConsole(final Portal portal, final Block console) {
        return new MenuItem(7, itemUnlinkConsole.item(), (player, menu, self) -> {
            menu.close(player);
            MenuFactory.confirm(
                getContext(),
                langUnlinkConsoleConfirmTitle.str(),
                itemUnlinkConsoleConfirmAccept.item(),
                player2 -> {
                    // Call event
                    final var event = new PortalUnlinkConsoleEvent(player2, console, portal, false);
                    getModule().getServer().getPluginManager().callEvent(event);
                    if (event.isCancelled() && !player2.hasPermission(getModule().adminPermission)) {
                        getModule().langUnlinkRestricted.send(player2);
                        return ClickResult.ERROR;
                    }

                    final var portalBlock = portal.portalBlockFor(console);
                    if (portalBlock == null) {
                        // The Console was likely already removed by another
                        // player
                        return ClickResult.ERROR;
                    }

                    getModule().removePortalBlock(portal, portalBlock);
                    return ClickResult.SUCCESS;
                },
                itemUnlinkConsoleConfirmCancel.item(),
                player2 -> menu.open(player2)
            )
                .tag(new PortalMenuTag(portal.id()))
                .open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemDestroyPortal(final Portal portal) {
        return new MenuItem(8, itemDestroyPortal.item(), (player, menu, self) -> {
            menu.close(player);
            MenuFactory.confirm(
                getContext(),
                langDestroyPortalConfirmTitle.str(),
                itemDestroyPortalConfirmAccept.item(),
                player2 -> {
                    // Call event
                    final var event = new PortalDestroyEvent(player2, portal, false);
                    getModule().getServer().getPluginManager().callEvent(event);
                    if (event.isCancelled() && !player2.hasPermission(getModule().adminPermission)) {
                        getModule().langDestroyRestricted.send(player2);
                        return ClickResult.ERROR;
                    }

                    getModule().removePortal(portal);
                    return ClickResult.SUCCESS;
                },
                itemDestroyPortalConfirmCancel.item(),
                player2 -> menu.open(player2)
            )
                .tag(new PortalMenuTag(portal.id()))
                .open(player);
            return ClickResult.SUCCESS;
        });
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
