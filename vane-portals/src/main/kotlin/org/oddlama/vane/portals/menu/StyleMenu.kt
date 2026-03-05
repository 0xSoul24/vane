package org.oddlama.vane.portals.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.BoundingBox
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.functional.Function1
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.*
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.Portals
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent
import org.oddlama.vane.portals.portal.Portal
import org.oddlama.vane.portals.portal.PortalBlock
import org.oddlama.vane.portals.portal.Style
import java.util.*

class StyleMenu(context: Context<Portals?>) : ModuleComponent<Portals?>(context.namespace("Style")) {
    @LangMessage
    var langTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockConsoleActiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockOriginActiveTitle: TranslatedMessage? = null

    @LangMessage
    @Suppress("unused")
    var langSelectBlockPortalActiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockBoundary1ActiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockBoundary2ActiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockBoundary3ActiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockBoundary4ActiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockBoundary5ActiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockConsoleInactiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockOriginInactiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockPortalInactiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockBoundary1InactiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockBoundary2InactiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockBoundary3InactiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockBoundary4InactiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectBlockBoundary5InactiveTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectStyleTitle: TranslatedMessage? = null

    @LangMessage
    var langFilterStylesTitle: TranslatedMessage? = null

    private val itemBlockConsoleActive: TranslatedItemStack<*>
    private val itemBlockOriginActive: TranslatedItemStack<*>
    private val itemBlockPortalActive: TranslatedItemStack<*>?
    private val itemBlockBoundary1Active: TranslatedItemStack<*>
    private val itemBlockBoundary2Active: TranslatedItemStack<*>
    private val itemBlockBoundary3Active: TranslatedItemStack<*>
    private val itemBlockBoundary4Active: TranslatedItemStack<*>
    private val itemBlockBoundary5Active: TranslatedItemStack<*>
    private val itemBlockConsoleInactive: TranslatedItemStack<*>
    private val itemBlockOriginInactive: TranslatedItemStack<*>
    private val itemBlockPortalInactive: TranslatedItemStack<*>
    private val itemBlockBoundary1Inactive: TranslatedItemStack<*>
    private val itemBlockBoundary2Inactive: TranslatedItemStack<*>
    private val itemBlockBoundary3Inactive: TranslatedItemStack<*>
    private val itemBlockBoundary4Inactive: TranslatedItemStack<*>
    private val itemBlockBoundary5Inactive: TranslatedItemStack<*>

    private val itemAccept: TranslatedItemStack<*>
    private val itemReset: TranslatedItemStack<*>
    private val itemSelectDefined: TranslatedItemStack<*>
    private val itemSelectStyle: TranslatedItemStack<*>
    private val itemCancel: TranslatedItemStack<*>

    init {
        val ctx = getContext()!!
        itemBlockConsoleActive = TranslatedItemStack<Portals?>(
            ctx,
            "BlockConsoleActive",
            Material.BARRIER,
            1,
            "Used to select active console block."
        )
        itemBlockOriginActive = TranslatedItemStack<Portals?>(
            ctx,
            "BlockOriginActive",
            Material.BARRIER,
            1,
            "Used to select active origin block."
        )
        itemBlockPortalActive = TranslatedItemStack<Portals?>(
            ctx,
            "BlockPortalActive",
            Material.BARRIER,
            1,
            "Used to select active portal area block. Defaults to end gateway if unset."
        )
        itemBlockBoundary1Active = TranslatedItemStack<Portals?>(
            ctx,
            "BlockBoundary1Active",
            Material.BARRIER,
            1,
            "Used to select active boundary variant 1 block."
        )
        itemBlockBoundary2Active = TranslatedItemStack<Portals?>(
            ctx,
            "BlockBoundary2Active",
            Material.BARRIER,
            1,
            "Used to select active boundary variant 2 block."
        )
        itemBlockBoundary3Active = TranslatedItemStack<Portals?>(
            ctx,
            "BlockBoundary3Active",
            Material.BARRIER,
            1,
            "Used to select active boundary variant 3 block."
        )
        itemBlockBoundary4Active = TranslatedItemStack<Portals?>(
            ctx,
            "BlockBoundary4Active",
            Material.BARRIER,
            1,
            "Used to select active boundary variant 4 block."
        )
        itemBlockBoundary5Active = TranslatedItemStack<Portals?>(
            ctx,
            "BlockBoundary5Active",
            Material.BARRIER,
            1,
            "Used to select active boundary variant 5 block."
        )
        itemBlockConsoleInactive = TranslatedItemStack<Portals?>(
            ctx,
            "BlockConsoleInactive",
            Material.BARRIER,
            1,
            "Used to select inactive console block."
        )
        itemBlockOriginInactive = TranslatedItemStack<Portals?>(
            ctx,
            "BlockOriginInactive",
            Material.BARRIER,
            1,
            "Used to select inactive origin block."
        )
        itemBlockPortalInactive = TranslatedItemStack<Portals?>(
            ctx,
            "BlockPortalInactive",
            Material.BARRIER,
            1,
            "Used to select inactive portal area block."
        )
        itemBlockBoundary1Inactive = TranslatedItemStack<Portals?>(
            ctx,
            "BlockBoundary1Inactive",
            Material.BARRIER,
            1,
            "Used to select inactive boundary variant 1 block."
        )
        itemBlockBoundary2Inactive = TranslatedItemStack<Portals?>(
            ctx,
            "BlockBoundary2Inactive",
            Material.BARRIER,
            1,
            "Used to select inactive boundary variant 2 block."
        )
        itemBlockBoundary3Inactive = TranslatedItemStack<Portals?>(
            ctx,
            "BlockBoundary3Inactive",
            Material.BARRIER,
            1,
            "Used to select inactive boundary variant 3 block."
        )
        itemBlockBoundary4Inactive = TranslatedItemStack<Portals?>(
            ctx,
            "BlockBoundary4Inactive",
            Material.BARRIER,
            1,
            "Used to select inactive boundary variant 4 block."
        )
        itemBlockBoundary5Inactive = TranslatedItemStack<Portals?>(
            ctx,
            "BlockBoundary5Inactive",
            Material.BARRIER,
            1,
            "Used to select inactive boundary variant 5 block."
        )

        itemAccept =
            TranslatedItemStack<Portals?>(ctx, "Accept", Material.LIME_TERRACOTTA, 1, "Used to apply the style.")
        itemReset = TranslatedItemStack<Portals?>(ctx, "Reset", Material.MILK_BUCKET, 1, "Used to reset any changes.")
        itemSelectDefined = TranslatedItemStack<Portals?>(
            ctx,
            "SelectDefined",
            Material.ITEM_FRAME,
            1,
            "Used to select a defined style from the configuration."
        )
        itemSelectStyle = TranslatedItemStack<Portals?>(
            ctx,
            "SelectStyle",
            Material.ITEM_FRAME,
            1,
            "Used to represent a defined style in the selector menu."
        )
        itemCancel = TranslatedItemStack<Portals?>(
            ctx,
            "Cancel",
            Material.RED_TERRACOTTA,
            1,
            "Used to abort style selection."
        )
    }

    fun create(portal: Portal, player: Player?, previous: Menu): Menu {
        val title = langTitle!!.strComponent("§5§l" + portal.name())
        val styleMenu = Menu(getContext()!!, Bukkit.createInventory(null, 4 * COLUMNS, title))
        styleMenu.tag(PortalMenuTag(portal.id()))

        val styleContainer = StyleContainer()
        styleContainer.definedStyle = portal.style()
        styleContainer.style = portal.copyStyle(module!!, null)

        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                0,
                itemBlockConsoleInactive,
                module!!.constructor.configMaterialConsole!!,
                langSelectBlockConsoleInactiveTitle!!.str(),
                PortalBlock.Type.CONSOLE,
                false
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                1,
                itemBlockOriginInactive,
                module!!.constructor.configMaterialOrigin!!,
                langSelectBlockOriginInactiveTitle!!.str(),
                PortalBlock.Type.ORIGIN,
                false
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                2,
                itemBlockPortalInactive,
                module!!.constructor.configMaterialPortalArea!!,
                langSelectBlockPortalInactiveTitle!!.str(),
                PortalBlock.Type.PORTAL,
                false
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                4,
                itemBlockBoundary1Inactive,
                module!!.constructor.configMaterialBoundary1!!,
                langSelectBlockBoundary1InactiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY1,
                false
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                5,
                itemBlockBoundary2Inactive,
                module!!.constructor.configMaterialBoundary2!!,
                langSelectBlockBoundary2InactiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY2,
                false
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                6,
                itemBlockBoundary3Inactive,
                module!!.constructor.configMaterialBoundary3!!,
                langSelectBlockBoundary3InactiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY3,
                false
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                7,
                itemBlockBoundary4Inactive,
                module!!.constructor.configMaterialBoundary4!!,
                langSelectBlockBoundary4InactiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY4,
                false
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                8,
                itemBlockBoundary5Inactive,
                module!!.constructor.configMaterialBoundary5!!,
                langSelectBlockBoundary5InactiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY5,
                false
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS,
                itemBlockConsoleActive,
                module!!.constructor.configMaterialConsole!!,
                langSelectBlockConsoleActiveTitle!!.str(),
                PortalBlock.Type.CONSOLE,
                true
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS + 1,
                itemBlockOriginActive,
                module!!.constructor.configMaterialOrigin!!,
                langSelectBlockOriginActiveTitle!!.str(),
                PortalBlock.Type.ORIGIN,
                true
            )
        )
        // styleMenu.add(menuItemBlockSelector(portal, styleContainer, 1 * COLUMNS
        // + 2, itemBlockPortalActive,
        // module!!.constructor.configMaterialPortalArea,
        // langSelectBlockPortalActiveTitle.str(), PortalBlock.Type.PORTAL, true));
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS + 4,
                itemBlockBoundary1Active,
                module!!.constructor.configMaterialBoundary1!!,
                langSelectBlockBoundary1ActiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY1,
                true
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS + 5,
                itemBlockBoundary2Active,
                module!!.constructor.configMaterialBoundary2!!,
                langSelectBlockBoundary2ActiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY2,
                true
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS + 6,
                itemBlockBoundary3Active,
                module!!.constructor.configMaterialBoundary3!!,
                langSelectBlockBoundary3ActiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY3,
                true
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS + 7,
                itemBlockBoundary4Active,
                module!!.constructor.configMaterialBoundary4!!,
                langSelectBlockBoundary4ActiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY4,
                true
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS + 8,
                itemBlockBoundary5Active,
                module!!.constructor.configMaterialBoundary5!!,
                langSelectBlockBoundary5ActiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY5,
                true
            )
        )

        styleMenu.add(menuItemAccept(portal, styleContainer, previous))
        styleMenu.add(menuItemReset(portal, styleContainer))
        styleMenu.add(menuItemSelectDefined(portal, styleContainer))
        styleMenu.add(menuItemCancel(previous))

        styleMenu.onNaturalClose { player2: Player? -> previous.open(player2!!) }
        return styleMenu
    }

    private fun menuItemBlockSelector(
        portal: Portal,
        styleContainer: StyleContainer,
        slot: Int,
        tItem: TranslatedItemStack<*>,
        buildingMaterial: Material,
        title: String,
        type: PortalBlock.Type,
        active: Boolean
    ): MenuWidget {
        return object : MenuItem(slot, null, Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
            menu!!.close(player!!)
            MenuFactory.itemSelector(
                getContext()!!,
                player,
                title,
                itemForType(styleContainer, active, type),
                true,
                { player2: Player?, item: ItemStack? ->
                    styleContainer.definedStyle = null
                    if (item == null) {
                        if (active && type == PortalBlock.Type.PORTAL) {
                            styleContainer.style!!.setMaterial(active, type, Material.END_GATEWAY, true)
                        }
                        styleContainer.style!!.setMaterial(active, type, Material.AIR, true)
                    } else {
                        styleContainer.style!!.setMaterial(active, type, item.type, true)
                    }
                    menu.open(player2!!)
                    Menu.ClickResult.SUCCESS
                },
                { player2: Player? -> menu.open(player2!!) },
                { item: ItemStack? ->
                    // Only allow placeable solid blocks
                    if (item == null || !(item.type.isBlock && item.type.isSolid())) {
                        return@itemSelector null
                    }

                    // Nothing from the blacklist
                    if (module!!.configBlacklistedMaterials?.contains(item.type) == true) {
                        return@itemSelector null
                    }

                    // Must be a block (should be given at this point)
                    val block = item.type.asBlockType() ?: return@itemSelector null

                    // Must be a full block with one AABB of extent (1,1,1)
                    val blockdata = block.createBlockData()
                    val voxelshape = blockdata.getCollisionShape(player.location)
                    val bbs = voxelshape.boundingBoxes
                    if (bbs.size != 1 ||
                        !bbs
                            .stream()
                            .allMatch { x: BoundingBox? -> x!!.widthX == 1.0 && x.widthZ == 1.0 && x.height == 1.0 }
                    ) {
                        return@itemSelector null
                    }

                    // Always select one
                    item.amount = 1
                    item
                }
            )
                .tag(PortalMenuTag(portal.id()))
                .open(player)
            menu.update()
            Menu.ClickResult.SUCCESS
        }) {
            override fun item(item: ItemStack?) {
                var stack: ItemStack = itemForType(styleContainer, active, type)
                if (stack.type == Material.AIR) {
                    stack = ItemStack(Material.BARRIER)
                }
                super.item(tItem.alternative(stack, "§6" + buildingMaterial.getKey()))
            }
        }
    }

    private fun menuItemAccept(
        portal: Portal,
        styleContainer: StyleContainer,
        previous: Menu
    ): MenuWidget {
        return MenuItem(3 * COLUMNS, itemAccept.item(), Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
            menu!!.close(player!!)
            val settingsEvent = PortalChangeSettingsEvent(player, portal, false)
            module!!.server.pluginManager.callEvent(settingsEvent)
            if (settingsEvent.isCancelled() && !player.hasPermission(module!!.adminPermission)) {
                return@Function3 Menu.ClickResult.ERROR
            }

            val s = styleContainer.style ?: return@Function3 Menu.ClickResult.ERROR
            portal.style(s)
            portal.updateBlocks(module!!)
            previous.open(player)
            Menu.ClickResult.SUCCESS
        })
    }

    private fun menuItemReset(portal: Portal, styleContainer: StyleContainer): MenuWidget {
        return MenuItem(3 * COLUMNS + 3, itemReset.item()) { _: Player?, menu: Menu?, _: MenuItem? ->
             styleContainer.style = portal.copyStyle(module!!, null)
             menu!!.update()
             Menu.ClickResult.SUCCESS
         }
    }

    private fun menuItemSelectDefined(portal: Portal, styleContainer: StyleContainer): MenuWidget {
        val itemFor: Function1<Style?, ItemStack?> = Function1 { style: Style? ->
            val mat = style!!.material(false, PortalBlock.Type.BOUNDARY1)
            if (mat == null) {
                return@Function1 ItemStack(Material.BARRIER)
            } else {
                return@Function1 ItemStack(mat)
            }
        }

        return MenuItem(
             3 * COLUMNS + 4,
             itemSelectDefined.item()
        ) { player: Player?, menu: Menu?, _: MenuItem? ->
            menu!!.close(player!!)
            val allStyles: ArrayList<Style?> = ArrayList<Style?>(module!!.styles.values)
            val filter = Filter.StringFilter({ s: Style?, str: String? ->
                s!!.key().toString().lowercase(
                    Locale.getDefault()
                ).contains(str!!)
            }
            )
            MenuFactory.genericSelector<Style?, Filter.StringFilter<Style?>?>(
                getContext()!!,
                player,
                langSelectStyleTitle!!.str(),
                langFilterStylesTitle!!.str(),
                allStyles,
                { s: Style? -> itemSelectStyle.alternative(itemFor.apply(s)!!, s!!.key()!!.key) },
                filter,
                { player2: Player?, m: Menu?, t: Style? ->
                    m!!.close(player2!!)
                    styleContainer.definedStyle = t!!.key()
                    styleContainer.style = t.copy(null)
                    menu.open(player2)
                    Menu.ClickResult.SUCCESS
                },
                { player2: Player? -> menu.open(player2!!) }
            )
                .tag(PortalMenuTag(portal.id()))
                .open(player)
            Menu.ClickResult.SUCCESS
        }
    }

    private fun menuItemCancel(previous: Menu): MenuWidget {
        return MenuItem(3 * COLUMNS + 8, itemCancel.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            menu!!.close(player!!)
            previous.open(player)
            Menu.ClickResult.SUCCESS
        }
    }

    public override fun onEnable() {}

    public override fun onDisable() {}

    private class StyleContainer {
        var definedStyle: NamespacedKey? = null
        var style: Style? = null
    }

    companion object {
        private const val COLUMNS = 9

        private fun itemForType(
            styleContainer: StyleContainer,
            active: Boolean,
            type: PortalBlock.Type
        ): ItemStack {
            if (active && type == PortalBlock.Type.PORTAL) {
                return ItemStack(Material.AIR)
            }
            return ItemStack(styleContainer.style!!.material(active, type)!!)
        }
    }
}
