package org.oddlama.vane.regions.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.*
import org.oddlama.vane.core.functional.Function2
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.regions.region.Region
import org.oddlama.vane.regions.region.RegionSelection
import org.oddlama.vane.regions.region.RegionGroup
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.Regions
import java.util.stream.Collectors
import kotlin.math.abs

class MainMenu(context: Context<Regions?>) : ModuleComponent<Regions?>(context.namespace("Main")) {
    @LangMessage
    var langTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectRegionTitle: TranslatedMessage? = null

    @LangMessage
    var langFilterRegionsTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectRegionGroupTitle: TranslatedMessage? = null

    @LangMessage
    var langFilterRegionGroupsTitle: TranslatedMessage? = null

    var itemCreateRegionStartSelection: TranslatedItemStack<*>
    var itemCreateRegionInvalidSelection: TranslatedItemStack<*>
    var itemCreateRegionValidSelection: TranslatedItemStack<*>
    var itemCancelSelection: TranslatedItemStack<*>
    var itemCurrentRegion: TranslatedItemStack<*>
    var itemListRegions: TranslatedItemStack<*>
    var itemSelectRegion: TranslatedItemStack<*>
    var itemCreateRegionGroup: TranslatedItemStack<*>
    var itemCurrentRegionGroup: TranslatedItemStack<*>
    var itemListRegionGroups: TranslatedItemStack<*>
    var itemSelectRegionGroup: TranslatedItemStack<*>

    init {
        val ctx = getContext()!!
        itemCreateRegionStartSelection = TranslatedItemStack<Regions?>(
            ctx,
            "CreateRegionStartSelection",
            Material.WRITABLE_BOOK,
            1,
            "Used to start creating a new region selection."
        )
        itemCreateRegionInvalidSelection = TranslatedItemStack<Regions?>(
            ctx,
            "CreateRegionInvalidSelection",
            Material.BARRIER,
            1,
            "Used to indicate an invalid selection."
        )
        itemCreateRegionValidSelection = TranslatedItemStack<Regions?>(
            ctx,
            "CreateRegionValidSelection",
            Material.WRITABLE_BOOK,
            1,
            "Used to create a new region with the current selection."
        )
        itemCancelSelection = TranslatedItemStack<Regions?>(
            ctx,
            "CancelSelection",
            Material.RED_TERRACOTTA,
            1,
            "Used to cancel region selection."
        )
        itemListRegions = TranslatedItemStack<Regions?>(
            ctx,
            "ListRegions",
            Material.COMPASS,
            1,
            "Used to select a region the player owns."
        )
        itemSelectRegion = TranslatedItemStack<Regions?>(
            ctx,
            "SelectRegion",
            Material.FILLED_MAP,
            1,
            "Used to represent a region in the region selection list."
        )
        itemCurrentRegion = TranslatedItemStack<Regions?>(
            ctx,
            "CurrentRegion",
            Material.FILLED_MAP,
            1,
            "Used to access the region the player currently stands in."
        )
        itemCreateRegionGroup = TranslatedItemStack<Regions?>(
            ctx,
            "CreateRegionGroup",
            Material.WRITABLE_BOOK,
            1,
            "Used to create a new region group."
        )
        itemListRegionGroups = TranslatedItemStack<Regions?>(
            ctx,
            "ListRegionGroups",
            Material.COMPASS,
            1,
            "Used to select a region group the player may administrate."
        )
        itemCurrentRegionGroup = TranslatedItemStack<Regions?>(
            ctx,
            "CurrentRegionGroup",
            Material.GLOBE_BANNER_PATTERN,
            1,
            "Used to access the region group associated with the region the player currently stands in."
        )
        itemSelectRegionGroup = TranslatedItemStack<Regions?>(
            ctx,
            "SelectRegionGroup",
            Material.GLOBE_BANNER_PATTERN,
            1,
            "Used to represent a region group in the region group selection list."
        )
    }

    fun create(player: Player): Menu {
        val columns = 9
        val title = langTitle!!.strComponent()
        val mainMenu = Menu(getContext()!!, Bukkit.createInventory(null, columns, title))

        val selectionMode: Boolean = module!!.isSelectingRegion(player)
        val region: Region? = module!!.regionAt(player.location)
        if (region != null) {
            mainMenu.tag(RegionMenuTag(region.id()))
        }

        // Check if target selection would be allowed
        if (selectionMode) {
            val selection: RegionSelection = module!!.getRegionSelection(player)
            mainMenu.add(menuItemCreateRegion(player, selection))
            mainMenu.add(menuItemCancelSelection())
        } else {
            mainMenu.add(menuItemStartSelection())
            mainMenu.add(menuItemListRegions())
            if (region != null && module!!.mayAdministrate(player, region)) {
                mainMenu.add(menuItemCurrentRegion(region))
            }
        }

        mainMenu.add(menuItemCreateRegionGroup())
        mainMenu.add(menuItemListRegionGroups())
        if (region != null) {
            val group = region.regionGroup(module!!)
            if (group != null && module!!.mayAdministrate(player, group)) {
                mainMenu.add(menuItemCurrentRegionGroup(group))
            }
        }

        return mainMenu
    }

    private fun menuItemStartSelection(): MenuWidget {
        return MenuItem(
            0,
            itemCreateRegionStartSelection.item()
        ) { player: Player?, menu: Menu?, _: MenuItem? ->
            val p = requireNotNull(player)
            menu!!.close(p)
            module!!.startRegionSelection(p)
            Menu.ClickResult.SUCCESS
        }
    }

    private fun menuItemCancelSelection(): MenuWidget {
        return MenuItem(1, itemCancelSelection.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            val p = requireNotNull(player)
            menu!!.close(p)
            module!!.cancelRegionSelection(p)
            Menu.ClickResult.SUCCESS
        }
    }

    private fun menuItemCreateRegion(finalPlayer: Player, selection: RegionSelection): MenuWidget {
        return object : MenuItem(0, null, Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
            if (selection.isValid(finalPlayer)) {
                val p = requireNotNull(player)
                menu!!.close(p)

                module!!.menus?.enterRegionNameMenu
                    ?.create(p, Function2 { player2: Player?, name: String? ->
                        if (player2 == null) return@Function2 Menu.ClickResult.ERROR
                        if (module!!.createRegionFromSelection(finalPlayer, name)) {
                            Menu.ClickResult.SUCCESS
                        } else {
                            Menu.ClickResult.ERROR
                        }
                    })
                    ?.onNaturalClose { player2: Player? -> player2?.let { menu.open(it) } }
                    ?.open(p)

                return@Function3 Menu.ClickResult.SUCCESS
            } else {
                return@Function3 Menu.ClickResult.ERROR
            }
        }) {
            override fun item(item: ItemStack?) {
                // Compute dimensions once if both corners are present
                val dims: Triple<Int, Int, Int>? = if (selection.primary != null && selection.secondary != null) {
                    Triple(
                        1 + abs(selection.primary!!.x - selection.secondary!!.x),
                        1 + abs(selection.primary!!.y - selection.secondary!!.y),
                        1 + abs(selection.primary!!.z - selection.secondary!!.z)
                    )
                } else null

                if (selection.isValid(finalPlayer)) {
                    val (dx, dy, dz) = dims!!
                    super.item(
                        itemCreateRegionValidSelection.item(
                            "§a$dx",
                            "§a$dy",
                            "§a$dz",
                            "§b" + module!!.configMinRegionExtentX,
                            "§b" + module!!.configMinRegionExtentY,
                            "§b" + module!!.configMinRegionExtentZ,
                            "§b" + module!!.configMaxRegionExtentX,
                            "§b" + module!!.configMaxRegionExtentY,
                            "§b" + module!!.configMaxRegionExtentZ,
                            "§a" + selection.price() + " §b" + module!!.currencyString()
                        )
                    )
                } else {
                    val isPrimarySet = selection.primary != null
                    val isSecondarySet = selection.secondary != null
                    val sameWorld =
                        isPrimarySet &&
                                isSecondarySet &&
                                selection.primary!!.world == selection.secondary!!.world

                    val minimumSatisfied: Boolean
                    val maximumSatisfied: Boolean
                    val noIntersection: Boolean
                    val canAfford: Boolean
                    val sdx: String?
                    val sdy: String?
                    val sdz: String?
                    val price: String?
                    if (isPrimarySet && isSecondarySet && sameWorld) {
                        val (dx, dy, dz) = dims!!
                        sdx = dx.toString()
                        sdy = dy.toString()
                        sdz = dz.toString()

                        minimumSatisfied =
                            dx >= module!!.configMinRegionExtentX && dy >= module!!.configMinRegionExtentY && dz >= module!!.configMinRegionExtentZ
                        maximumSatisfied =
                            dx <= module!!.configMaxRegionExtentX && dy <= module!!.configMaxRegionExtentY && dz <= module!!.configMaxRegionExtentZ
                        noIntersection = !selection.intersectsExisting()
                        canAfford = selection.canAfford(finalPlayer)
                        price =
                            (if (canAfford) "§a" else "§c") + selection.price() + " §b" + module!!.currencyString()
                    } else {
                        sdx = "§7?"
                        sdy = "§7?"
                        sdz = "§7?"
                        minimumSatisfied = false
                        maximumSatisfied = false
                        noIntersection = true
                        canAfford = false
                        price = "§7?"
                    }

                    val extentColor = if (minimumSatisfied && maximumSatisfied) "§a" else "§c"
                    super.item(
                        itemCreateRegionInvalidSelection.item(
                            if (isPrimarySet) "§a✓" else "§c✕",
                            if (isSecondarySet) "§a✓" else "§c✕",
                            if (sameWorld) "§a✓" else "§c✕",
                            if (noIntersection) "§a✓" else "§c✕",
                            if (minimumSatisfied) "§a✓" else "§c✕",
                            if (maximumSatisfied) "§a✓" else "§c✕",
                            if (canAfford) "§a✓" else "§c✕",
                            extentColor + sdx,
                            extentColor + sdy,
                            extentColor + sdz,
                            "§b" + module!!.configMinRegionExtentX,
                            "§b" + module!!.configMinRegionExtentY,
                            "§b" + module!!.configMinRegionExtentZ,
                            "§b" + module!!.configMaxRegionExtentX,
                            "§b" + module!!.configMaxRegionExtentY,
                            "§b" + module!!.configMaxRegionExtentZ,
                            price
                        )
                    )
                }
            }
        }
    }

    private fun menuItemListRegions(): MenuWidget {
        return MenuItem(1, itemListRegions.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            val p = player!!
            menu!!.close(p)
            val allRegions: MutableList<Region?> = module!!
                .allRegions()
                .stream()
                .filter { r: Region? -> r != null && module!!.mayAdministrate(p, r) }
                .map { r: Region? -> r!! }
                .sorted { a: Region, b: Region -> a.name()!!.compareTo(b.name()!!, ignoreCase = true) }
                .collect(Collectors.toList())

            val filter = nameFilter { r: Region? -> r?.name() }
            MenuFactory.genericSelector<Region?, Filter.StringFilter<Region?>?>(
                getContext()!!,
                p,
                langSelectRegionTitle!!.str(),
                langFilterRegionsTitle!!.str(),
                allRegions,
                { r: Region? -> itemSelectRegion.item("§a§l" + r!!.name()) },
                filter,
                { player2: Player?, m: Menu?, region: Region? ->
                    val p2 = player2!!
                    m!!.close(p2)
                    module!!.menus?.regionMenu?.create(region!!, p2)?.open(p2)
                    Menu.ClickResult.SUCCESS
                },
                { player2: Player? -> player2?.let { menu.open(it) } }
            ).open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    private fun menuItemCurrentRegion(region: Region): MenuWidget {
        return MenuItem(
            2,
            itemCurrentRegion.item("§a§l" + region.name())
        ) { player: Player?, menu: Menu?, _: MenuItem? ->
            val p = player!!
            menu!!.close(p)
            module!!.menus?.regionMenu?.create(region, p)?.open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    private fun menuItemCreateRegionGroup(): MenuWidget {
        return MenuItem(7, itemCreateRegionGroup.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            val p = requireNotNull(player)
            menu!!.close(p)
            module!!.menus?.enterRegionGroupNameMenu
                ?.create(p, Function2 { player2: Player?, name: String? ->
                    if (player2 == null) return@Function2 Menu.ClickResult.ERROR
                    val group = RegionGroup(name, player2.uniqueId)
                    module!!.addRegionGroup(group)
                    player2.let { module!!.menus?.regionGroupMenu?.create(group, it)?.open(it) }
                    Menu.ClickResult.SUCCESS
                })
                ?.onNaturalClose { player2: Player? -> player2?.let { menu.open(it) } }
                ?.open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    private fun menuItemListRegionGroups(): MenuWidget {
        return MenuItem(8, itemListRegionGroups.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            val p = player!!
            menu!!.close(p)
            val (allRegionGroups, filter) = module!!.administrableRegionGroups(p)
            MenuFactory.genericSelector<RegionGroup?, Filter.StringFilter<RegionGroup?>?>(
                getContext()!!,
                p,
                langSelectRegionGroupTitle!!.str(),
                langFilterRegionGroupsTitle!!.str(),
                allRegionGroups,
                { r: RegionGroup? -> itemSelectRegionGroup.item("§a§l" + r!!.name()) },
                filter,
                { player2: Player?, m: Menu?, group: RegionGroup? ->
                    val p2 = player2!!
                    m!!.close(p2)
                    module!!.menus?.regionGroupMenu?.create(group!!, p2)?.open(p2)
                    Menu.ClickResult.SUCCESS
                },
                { player2: Player? -> player2?.let { menu.open(it) } }
            ).open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    private fun menuItemCurrentRegionGroup(regionGroup: RegionGroup): MenuWidget {
        return MenuItem(
            6,
            itemCurrentRegionGroup.item("§a§l" + regionGroup.name())
        ) { player: Player?, menu: Menu?, _: MenuItem? ->
            val p = requireNotNull(player)
            menu!!.close(p)
            module!!.menus?.regionGroupMenu?.create(regionGroup, p)?.open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    public override fun onEnable() {}

    public override fun onDisable() {}
}

