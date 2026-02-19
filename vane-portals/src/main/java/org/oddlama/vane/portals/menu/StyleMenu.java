package org.oddlama.vane.portals.menu;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.oddlama.vane.core.functional.Function1;
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
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.Style;

public class StyleMenu extends ModuleComponent<Portals> {

    private static final int columns = 9;

    @LangMessage
    public TranslatedMessage langTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockConsoleActiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockOriginActiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockPortalActiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockBoundary1ActiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockBoundary2ActiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockBoundary3ActiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockBoundary4ActiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockBoundary5ActiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockConsoleInactiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockOriginInactiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockPortalInactiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockBoundary1InactiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockBoundary2InactiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockBoundary3InactiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockBoundary4InactiveTitle;

    @LangMessage
    public TranslatedMessage langSelectBlockBoundary5InactiveTitle;

    @LangMessage
    public TranslatedMessage langSelectStyleTitle;

    @LangMessage
    public TranslatedMessage langFilterStylesTitle;

    private TranslatedItemStack<?> itemBlockConsoleActive;
    private TranslatedItemStack<?> itemBlockOriginActive;
    private TranslatedItemStack<?> itemBlockPortalActive;
    private TranslatedItemStack<?> itemBlockBoundary1Active;
    private TranslatedItemStack<?> itemBlockBoundary2Active;
    private TranslatedItemStack<?> itemBlockBoundary3Active;
    private TranslatedItemStack<?> itemBlockBoundary4Active;
    private TranslatedItemStack<?> itemBlockBoundary5Active;
    private TranslatedItemStack<?> itemBlockConsoleInactive;
    private TranslatedItemStack<?> itemBlockOriginInactive;
    private TranslatedItemStack<?> itemBlockPortalInactive;
    private TranslatedItemStack<?> itemBlockBoundary1Inactive;
    private TranslatedItemStack<?> itemBlockBoundary2Inactive;
    private TranslatedItemStack<?> itemBlockBoundary3Inactive;
    private TranslatedItemStack<?> itemBlockBoundary4Inactive;
    private TranslatedItemStack<?> itemBlockBoundary5Inactive;

    private TranslatedItemStack<?> itemAccept;
    private TranslatedItemStack<?> itemReset;
    private TranslatedItemStack<?> itemSelectDefined;
    private TranslatedItemStack<?> itemSelectStyle;
    private TranslatedItemStack<?> itemCancel;

    public StyleMenu(Context<Portals> context) {
        super(context.namespace("Style"));
        final var ctx = getContext();
        itemBlockConsoleActive = new TranslatedItemStack<>(
            ctx,
            "BlockConsoleActive",
            Material.BARRIER,
            1,
            "Used to select active console block."
        );
        itemBlockOriginActive = new TranslatedItemStack<>(
            ctx,
            "BlockOriginActive",
            Material.BARRIER,
            1,
            "Used to select active origin block."
        );
        itemBlockPortalActive = new TranslatedItemStack<>(
            ctx,
            "BlockPortalActive",
            Material.BARRIER,
            1,
            "Used to select active portal area block. Defaults to end gateway if unset."
        );
        itemBlockBoundary1Active = new TranslatedItemStack<>(
            ctx,
            "BlockBoundary1Active",
            Material.BARRIER,
            1,
            "Used to select active boundary variant 1 block."
        );
        itemBlockBoundary2Active = new TranslatedItemStack<>(
            ctx,
            "BlockBoundary2Active",
            Material.BARRIER,
            1,
            "Used to select active boundary variant 2 block."
        );
        itemBlockBoundary3Active = new TranslatedItemStack<>(
            ctx,
            "BlockBoundary3Active",
            Material.BARRIER,
            1,
            "Used to select active boundary variant 3 block."
        );
        itemBlockBoundary4Active = new TranslatedItemStack<>(
            ctx,
            "BlockBoundary4Active",
            Material.BARRIER,
            1,
            "Used to select active boundary variant 4 block."
        );
        itemBlockBoundary5Active = new TranslatedItemStack<>(
            ctx,
            "BlockBoundary5Active",
            Material.BARRIER,
            1,
            "Used to select active boundary variant 5 block."
        );
        itemBlockConsoleInactive = new TranslatedItemStack<>(
            ctx,
            "BlockConsoleInactive",
            Material.BARRIER,
            1,
            "Used to select inactive console block."
        );
        itemBlockOriginInactive = new TranslatedItemStack<>(
            ctx,
            "BlockOriginInactive",
            Material.BARRIER,
            1,
            "Used to select inactive origin block."
        );
        itemBlockPortalInactive = new TranslatedItemStack<>(
            ctx,
            "BlockPortalInactive",
            Material.BARRIER,
            1,
            "Used to select inactive portal area block."
        );
        itemBlockBoundary1Inactive = new TranslatedItemStack<>(
            ctx,
            "BlockBoundary1Inactive",
            Material.BARRIER,
            1,
            "Used to select inactive boundary variant 1 block."
        );
        itemBlockBoundary2Inactive = new TranslatedItemStack<>(
            ctx,
            "BlockBoundary2Inactive",
            Material.BARRIER,
            1,
            "Used to select inactive boundary variant 2 block."
        );
        itemBlockBoundary3Inactive = new TranslatedItemStack<>(
            ctx,
            "BlockBoundary3Inactive",
            Material.BARRIER,
            1,
            "Used to select inactive boundary variant 3 block."
        );
        itemBlockBoundary4Inactive = new TranslatedItemStack<>(
            ctx,
            "BlockBoundary4Inactive",
            Material.BARRIER,
            1,
            "Used to select inactive boundary variant 4 block."
        );
        itemBlockBoundary5Inactive = new TranslatedItemStack<>(
            ctx,
            "BlockBoundary5Inactive",
            Material.BARRIER,
            1,
            "Used to select inactive boundary variant 5 block."
        );

        itemAccept = new TranslatedItemStack<>(ctx, "Accept", Material.LIME_TERRACOTTA, 1, "Used to apply the style.");
        itemReset = new TranslatedItemStack<>(ctx, "Reset", Material.MILK_BUCKET, 1, "Used to reset any changes.");
        itemSelectDefined = new TranslatedItemStack<>(
            ctx,
            "SelectDefined",
            Material.ITEM_FRAME,
            1,
            "Used to select a defined style from the configuration."
        );
        itemSelectStyle = new TranslatedItemStack<>(
            ctx,
            "SelectStyle",
            Material.ITEM_FRAME,
            1,
            "Used to represent a defined style in the selector menu."
        );
        itemCancel = new TranslatedItemStack<>(
            ctx,
            "Cancel",
            Material.RED_TERRACOTTA,
            1,
            "Used to abort style selection."
        );
    }

    public Menu create(final Portal portal, final Player player, final Menu previous) {
        final var title = langTitle.strComponent("§5§l" + portal.name());
        final var styleMenu = new Menu(getContext(), Bukkit.createInventory(null, 4 * columns, title));
        styleMenu.tag(new PortalMenuTag(portal.id()));

        final var styleContainer = new StyleContainer();
        styleContainer.definedStyle = portal.style();
        styleContainer.style = portal.copyStyle(getModule(), null);

        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                0,
                    itemBlockConsoleInactive,
                getModule().constructor.configMaterialConsole,
                langSelectBlockConsoleInactiveTitle.str(),
                PortalBlock.Type.CONSOLE,
                false
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                1,
                    itemBlockOriginInactive,
                getModule().constructor.configMaterialOrigin,
                langSelectBlockOriginInactiveTitle.str(),
                PortalBlock.Type.ORIGIN,
                false
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                2,
                    itemBlockPortalInactive,
                getModule().constructor.configMaterialPortalArea,
                langSelectBlockPortalInactiveTitle.str(),
                PortalBlock.Type.PORTAL,
                false
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                4,
                    itemBlockBoundary1Inactive,
                getModule().constructor.configMaterialBoundary1,
                langSelectBlockBoundary1InactiveTitle.str(),
                PortalBlock.Type.BOUNDARY1,
                false
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                5,
                    itemBlockBoundary2Inactive,
                getModule().constructor.configMaterialBoundary2,
                langSelectBlockBoundary2InactiveTitle.str(),
                PortalBlock.Type.BOUNDARY2,
                false
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                6,
                    itemBlockBoundary3Inactive,
                getModule().constructor.configMaterialBoundary3,
                langSelectBlockBoundary3InactiveTitle.str(),
                PortalBlock.Type.BOUNDARY3,
                false
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                7,
                    itemBlockBoundary4Inactive,
                getModule().constructor.configMaterialBoundary4,
                langSelectBlockBoundary4InactiveTitle.str(),
                PortalBlock.Type.BOUNDARY4,
                false
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                8,
                    itemBlockBoundary5Inactive,
                getModule().constructor.configMaterialBoundary5,
                langSelectBlockBoundary5InactiveTitle.str(),
                PortalBlock.Type.BOUNDARY5,
                false
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                columns,
                    itemBlockConsoleActive,
                getModule().constructor.configMaterialConsole,
                langSelectBlockConsoleActiveTitle.str(),
                PortalBlock.Type.CONSOLE,
                true
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                columns + 1,
                    itemBlockOriginActive,
                getModule().constructor.configMaterialOrigin,
                langSelectBlockOriginActiveTitle.str(),
                PortalBlock.Type.ORIGIN,
                true
            )
        );
        // styleMenu.add(menuItemBlockSelector(portal, styleContainer, 1 * columns
        // + 2, itemBlockPortalActive,
        // getModule().constructor.configMaterialPortalArea,
        // langSelectBlockPortalActiveTitle.str(), PortalBlock.Type.PORTAL, true));
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                columns + 4,
                    itemBlockBoundary1Active,
                getModule().constructor.configMaterialBoundary1,
                langSelectBlockBoundary1ActiveTitle.str(),
                PortalBlock.Type.BOUNDARY1,
                true
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                columns + 5,
                    itemBlockBoundary2Active,
                getModule().constructor.configMaterialBoundary2,
                langSelectBlockBoundary2ActiveTitle.str(),
                PortalBlock.Type.BOUNDARY2,
                true
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                columns + 6,
                    itemBlockBoundary3Active,
                getModule().constructor.configMaterialBoundary3,
                langSelectBlockBoundary3ActiveTitle.str(),
                PortalBlock.Type.BOUNDARY3,
                true
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                columns + 7,
                    itemBlockBoundary4Active,
                getModule().constructor.configMaterialBoundary4,
                langSelectBlockBoundary4ActiveTitle.str(),
                PortalBlock.Type.BOUNDARY4,
                true
            )
        );
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                columns + 8,
                    itemBlockBoundary5Active,
                getModule().constructor.configMaterialBoundary5,
                langSelectBlockBoundary5ActiveTitle.str(),
                PortalBlock.Type.BOUNDARY5,
                true
            )
        );

        styleMenu.add(menuItemAccept(portal, styleContainer, previous));
        styleMenu.add(menuItemReset(portal, styleContainer));
        styleMenu.add(menuItemSelectDefined(portal, styleContainer));
        styleMenu.add(menuItemCancel(previous));

        styleMenu.onNaturalClose(player2 -> previous.open(player2));
        return styleMenu;
    }

    private static ItemStack itemForType(
        final StyleContainer styleContainer,
        final boolean active,
        final PortalBlock.Type type
    ) {
        if (active && type == PortalBlock.Type.PORTAL) {
            return new ItemStack(Material.AIR);
        }
        return new ItemStack(styleContainer.style.material(active, type));
    }

    private MenuWidget menuItemBlockSelector(
        final Portal portal,
        final StyleContainer styleContainer,
        int slot,
        final TranslatedItemStack<?> tItem,
        final Material buildingMaterial,
        final String title,
        final PortalBlock.Type type,
        final boolean active
    ) {
        return new MenuItem(slot, null, (player, menu, self) -> {
            menu.close(player);
            MenuFactory.itemSelector(
                getContext(),
                player,
                title,
                itemForType(styleContainer, active, type),
                true,
                (player2, item) -> {
                    styleContainer.definedStyle = null;
                    if (item == null) {
                        if (active && type == PortalBlock.Type.PORTAL) {
                            styleContainer.style.setMaterial(active, type, Material.END_GATEWAY, true);
                        }
                        styleContainer.style.setMaterial(active, type, Material.AIR, true);
                    } else {
                        styleContainer.style.setMaterial(active, type, item.getType(), true);
                    }
                    menu.open(player2);
                    return ClickResult.SUCCESS;
                },
                player2 -> menu.open(player2),
                item -> {
                    // Only allow placeable solid blocks
                    if (item == null || !(item.getType().isBlock() && item.getType().isSolid())) {
                        return null;
                    }

                    // Nothing from the blacklist
                    if (getModule().configBlacklistedMaterials.contains(item.getType())) {
                        return null;
                    }

                    // Must be a block (should be given at this point)
                    var block = item.getType().asBlockType();
                    if (block == null) {
                        return null;
                    }

                    // Must be a full block with one AABB of extent (1,1,1)
                    var blockdata = block.createBlockData();
                    var voxelshape = blockdata.getCollisionShape(player.getLocation());
                    var bbs = voxelshape.getBoundingBoxes();
                    if (
                        bbs.size() != 1 ||
                        !bbs
                            .stream()
                            .allMatch(x -> x.getWidthX() == 1.0 && x.getWidthZ() == 1.0 && x.getHeight() == 1.0)
                    ) {
                        return null;
                    }

                    // Always select one
                    item.setAmount(1);
                    return item;
                }
            )
                .tag(new PortalMenuTag(portal.id()))
                .open(player);
            menu.update();
            return ClickResult.SUCCESS;
        }) {
            @Override
            public void item(final ItemStack item) {
                var stack = itemForType(styleContainer, active, type);
                if (stack.getType() == Material.AIR) {
                    stack = new ItemStack(Material.BARRIER);
                }
                super.item(tItem.alternative(stack, "§6" + buildingMaterial.getKey()));
            }
        };
    }

    private MenuWidget menuItemAccept(
        final Portal portal,
        final StyleContainer styleContainer,
        final Menu previous
    ) {
        return new MenuItem(3 * columns, itemAccept.item(), (player, menu, self) -> {
            menu.close(player);

            final var settingsEvent = new PortalChangeSettingsEvent(player, portal, false);
            getModule().getServer().getPluginManager().callEvent(settingsEvent);
            if (settingsEvent.isCancelled() && !player.hasPermission(getModule().adminPermission)) {
                return ClickResult.ERROR;
            }

            portal.style(styleContainer.style);
            portal.updateBlocks(getModule());
            previous.open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemReset(final Portal portal, final StyleContainer styleContainer) {
        return new MenuItem(3 * columns + 3, itemReset.item(), (player, menu, self) -> {
            styleContainer.style = portal.copyStyle(getModule(), null);
            menu.update();
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menuItemSelectDefined(final Portal portal, final StyleContainer styleContainer) {
        final Function1<Style, ItemStack> itemFor = style -> {
            final var mat = style.material(false, PortalBlock.Type.BOUNDARY1);
            if (mat == null) {
                return new ItemStack(Material.BARRIER);
            } else {
                return new ItemStack(mat);
            }
        };

        return new MenuItem(3 * columns + 4, itemSelectDefined.item(), (player, menu, self) -> {
            menu.close(player);
            final var allStyles = new ArrayList<>(getModule().styles.values());
            final var filter = new Filter.StringFilter<Style>((s, str) -> s.key().toString().toLowerCase().contains(str)
            );
            MenuFactory.genericSelector(
                getContext(),
                player,
                langSelectStyleTitle.str(),
                langFilterStylesTitle.str(),
                allStyles,
                s -> itemSelectStyle.alternative(itemFor.apply(s), s.key().getKey()),
                filter,
                (player2, m, t) -> {
                    m.close(player2);
                    styleContainer.definedStyle = t.key();
                    styleContainer.style = t.copy(null);
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

    private MenuWidget menuItemCancel(final Menu previous) {
        return new MenuItem(3 * columns + 8, itemCancel.item(), (player, menu, self) -> {
            menu.close(player);
            previous.open(player);
            return ClickResult.SUCCESS;
        });
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

    private static class StyleContainer {

        public NamespacedKey definedStyle = null;
        public Style style = null;
    }
}
