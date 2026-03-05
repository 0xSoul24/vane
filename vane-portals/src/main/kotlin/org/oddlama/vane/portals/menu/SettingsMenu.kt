package org.oddlama.vane.portals.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.functional.Function2
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.functional.Function4
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.menu.MenuFactory
import org.oddlama.vane.core.menu.MenuItem
import org.oddlama.vane.core.menu.MenuWidget
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.Portals
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent
import org.oddlama.vane.portals.portal.Portal
import org.oddlama.vane.util.StorageUtil

class SettingsMenu(context: Context<Portals?>) : ModuleComponent<Portals?>(context.namespace("Settings")) {
    @LangMessage
    var langTitle: TranslatedMessage? = null

    @LangMessage
    var langSelectIconTitle: TranslatedMessage? = null

    var itemRename: TranslatedItemStack<*>
    var itemSelectIcon: TranslatedItemStack<*>
    var itemSelectStyle: TranslatedItemStack<*>
    var itemExitOrientationLockOn: TranslatedItemStack<*>
    var itemExitOrientationLockOff: TranslatedItemStack<*>
    var itemVisibilityPublic: TranslatedItemStack<*>
    var itemVisibilityGroup: TranslatedItemStack<*>
    var itemVisibilityGroupInternal: TranslatedItemStack<*>
    var itemVisibilityPrivate: TranslatedItemStack<*>
    var itemTargetLockOn: TranslatedItemStack<*>
    var itemTargetLockOff: TranslatedItemStack<*>
    var itemBack: TranslatedItemStack<*>

    init {
        val ctx = getContext()!!
        itemRename = TranslatedItemStack<Portals?>(ctx, "Rename", Material.NAME_TAG, 1, "Used to rename the portal.")
        itemSelectIcon = TranslatedItemStack<Portals?>(
            ctx,
            "SelectIcon",
            StorageUtil.namespacedKey("vane", "decoration_end_portal_orb"),
            1,
            "Used to select the portal's icon."
        )
        itemSelectStyle = TranslatedItemStack<Portals?>(
            ctx,
            "SelectStyle",
            Material.ITEM_FRAME,
            1,
            "Used to change the portal's style."
        )
        itemExitOrientationLockOn = TranslatedItemStack<Portals?>(
            ctx,
            "ExitOrientationLockOn",
            Material.SOUL_TORCH,
            1,
            "Used to toggle and indicate enabled exit orientation lock."
        )
        itemExitOrientationLockOff = TranslatedItemStack<Portals?>(
            ctx,
            "ExitOrientationLockOff",
            Material.TORCH,
            1,
            "Used to toggle and indicate disabled exit orientation lock."
        )
        itemVisibilityPublic = TranslatedItemStack<Portals?>(
            ctx,
            "VisibilityPublic",
            Material.ENDER_EYE,
            1,
            "Used to change and indicate public visibility."
        )
        itemVisibilityGroup = TranslatedItemStack<Portals?>(
            ctx,
            "VisibilityGroup",
            Material.ENDER_PEARL,
            1,
            "Used to change and indicate group visibility."
        )
        itemVisibilityGroupInternal = TranslatedItemStack<Portals?>(
            ctx,
            "VisibilityGroupInternal",
            Material.FIRE_CHARGE,
            1,
            "Used to change and indicate group internal visibility."
        )
        itemVisibilityPrivate = TranslatedItemStack<Portals?>(
            ctx,
            "VisibilityPrivate",
            Material.FIREWORK_STAR,
            1,
            "Used to change and indicate private visibility."
        )
        itemTargetLockOn = TranslatedItemStack<Portals?>(
            ctx,
            "TargetLockOn",
            Material.SLIME_BALL,
            1,
            "Used to toggle and indicate enabled target lock."
        )
        itemTargetLockOff = TranslatedItemStack<Portals?>(
            ctx,
            "TargetLockOff",
            Material.SNOWBALL,
            1,
            "Used to toggle and indicate disabled target lock."
        )
        itemBack = TranslatedItemStack<Portals?>(
            ctx,
            "Back",
            Material.PRISMARINE_SHARD,
            1,
            "Used to go back to the previous menu."
        )
    }

    // Helper: centralize settings-change permission/event check and notify player if blocked
    private fun checkSettingsOrNotify(player: Player, portal: Portal): Boolean {
        val evt = PortalChangeSettingsEvent(player, portal, false)
        module!!.server.pluginManager.callEvent(evt)
        if (evt.isCancelled && !player.hasPermission(module!!.adminPermission)) {
            module!!.langSettingsRestricted?.send(player)
            return false
        }
        return true
    }

    // HINT: We don't capture the previous menu and open a new one on exit,
    // to correctly reflect changes done in here. (e.g., menu title due to portal name)
    fun create(portal: Portal, player: Player?, console: Block?): Menu {
        val columns = 9
        val title = langTitle!!.strComponent("§5§l" + portal.name())
        val settingsMenu = Menu(getContext()!!, Bukkit.createInventory(null, columns, title))
        settingsMenu.tag(PortalMenuTag(portal.id()))

        settingsMenu.add(menuItemRename(portal, console))
        settingsMenu.add(menuItemSelectIcon(portal))
        settingsMenu.add(menuItemSelectStyle(portal))
        settingsMenu.add(menuItemExitOrientationLock(portal))
        settingsMenu.add(menuItemVisibility(portal))
        settingsMenu.add(menuItemTargetLock(portal))
        settingsMenu.add(menuItemBack(portal, console))

        settingsMenu.onNaturalClose { player2: Player? ->
            // Use safe-let to avoid unnecessary !! assertions
            player2?.let { p ->
                module!!.menus?.consoleMenu?.create(portal, p, console)?.open(p)
            }
        }

        return settingsMenu
    }

    private fun menuItemRename(portal: Portal, console: Block?): MenuWidget {
        return MenuItem(0, itemRename.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            val p = player!!
            menu!!.close(p)
            // Ensure enterNameMenu exists; chain calls safely because menus may be nullable
            module!!.menus?.enterNameMenu
                ?.create(p, portal.name() ?: "", Function2 { player2: Player?, name: String? ->
                    if (player2 == null || !checkSettingsOrNotify(player2, portal)) return@Function2 Menu.ClickResult.ERROR

                    portal.name(name)

                    // Update portal icons to reflect new name
                    module!!.updatePortalIcon(portal)

                    // Open new menu because of possibly changed title
                    module!!.menus?.settingsMenu?.create(portal, player2, console)?.open(player2)
                    Menu.ClickResult.SUCCESS
                })
                ?.onNaturalClose { player2: Player? ->
                    player2?.let { q -> module!!.menus?.settingsMenu?.create(portal, q, console)?.open(q) }
                }
                ?.open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    private fun menuItemSelectIcon(portal: Portal): MenuWidget {
        return MenuItem(1, itemSelectIcon.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            val p = player!!
            menu!!.close(p)
            MenuFactory.itemSelector(
                getContext()!!,
                p,
                langSelectIconTitle!!.str(),
                portal.icon(),
                true,
                { player2: Player?, item: ItemStack? ->
                    if (player2 == null || !checkSettingsOrNotify(player2, portal)) return@itemSelector Menu.ClickResult.ERROR

                    portal.icon(item)
                    module!!.updatePortalIcon(portal)
                    menu.open(player2)
                    Menu.ClickResult.SUCCESS
                },
                { player2: Player? -> player2?.let { menu.open(it) } }
            )
                .tag(PortalMenuTag(portal.id()))
                .open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    private fun menuItemSelectStyle(portal: Portal): MenuWidget {
        return MenuItem(2, itemSelectStyle.item(), Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
            if (player == null || !checkSettingsOrNotify(player, portal)) return@Function3 Menu.ClickResult.ERROR

            menu!!.close(player)
            val mg = module!!.menus ?: return@Function3 Menu.ClickResult.ERROR
            val sm = mg.styleMenu ?: return@Function3 Menu.ClickResult.ERROR
            sm.create(portal, player, menu).open(player)
            Menu.ClickResult.SUCCESS
        })
    }

    private fun menuItemExitOrientationLock(portal: Portal): MenuWidget {
        return object : MenuItem(4, null, Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
            if (player == null || !checkSettingsOrNotify(player, portal)) return@Function3 Menu.ClickResult.ERROR

            portal.exitOrientationLocked(!portal.exitOrientationLocked())
            menu!!.update()
            Menu.ClickResult.SUCCESS
        }) {
            override fun item(item: ItemStack?) {
                if (portal.exitOrientationLocked()) {
                    super.item(itemExitOrientationLockOn.item())
                } else {
                    super.item(itemExitOrientationLockOff.item())
                }
            }
        }
    }

    private fun menuItemVisibility(portal: Portal): MenuWidget {
        return object :
            MenuItem(5, null, Function4 { player: Player?, menu: Menu?, _: MenuItem?, event: InventoryClickEvent? ->
                if (!Menu.isLeftOrRightClick(event)) {
                    return@Function4 Menu.ClickResult.INVALID_CLICK
                }
                if (player == null || !checkSettingsOrNotify(player, portal)) return@Function4 Menu.ClickResult.ERROR

                var newVis = portal.visibility() ?: Portal.Visibility.PRIVATE
                // If the "regions" plugin is not installed, we need to skip group visibility.
                do {
                    newVis = if (event!!.click == ClickType.RIGHT) newVis.prev() else newVis.next()
                } while (newVis.requiresRegions() && !module!!.isRegionsInstalled)

                portal.visibility(newVis)
                module!!.updatePortalVisibility(portal)
                menu!!.update()
                Menu.ClickResult.SUCCESS
            }) {
            override fun item(item: ItemStack?) {
                when (portal.visibility() ?: Portal.Visibility.PRIVATE) {
                    Portal.Visibility.PUBLIC -> super.item(itemVisibilityPublic.item())
                    Portal.Visibility.GROUP -> super.item(itemVisibilityGroup.item())
                    Portal.Visibility.GROUP_INTERNAL -> super.item(itemVisibilityGroupInternal.item())
                    Portal.Visibility.PRIVATE -> super.item(itemVisibilityPrivate.item())
                }
            }
        }
    }

    private fun menuItemTargetLock(portal: Portal): MenuWidget {
        return object : MenuItem(6, null, Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
            if (player == null || !checkSettingsOrNotify(player, portal)) return@Function3 Menu.ClickResult.ERROR

            portal.targetLocked(!portal.targetLocked())
            menu!!.update()
            Menu.ClickResult.SUCCESS
        }) {
            override fun item(item: ItemStack?) {
                if (portal.targetLocked()) {
                    super.item(itemTargetLockOn.item())
                } else {
                    super.item(itemTargetLockOff.item())
                }
            }
        }
    }

    private fun menuItemBack(portal: Portal, console: Block?): MenuWidget {
        return MenuItem(8, itemBack.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            if (player == null) return@MenuItem Menu.ClickResult.ERROR
            menu!!.close(player)
            module!!.menus?.consoleMenu?.create(portal, player, console)?.open(player)
            Menu.ClickResult.SUCCESS
        }
    }

    public override fun onEnable() {}

    public override fun onDisable() {}
}
