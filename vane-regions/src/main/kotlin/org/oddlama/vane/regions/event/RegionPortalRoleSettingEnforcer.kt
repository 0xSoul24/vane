package org.oddlama.vane.regions.event

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.portals.event.*
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.regions.region.Region
import org.oddlama.vane.regions.region.RoleSetting

class RegionPortalRoleSettingEnforcer(context: Context<Regions?>?) : Listener<Regions?>(context) {
    fun checkSettingAt(
        location: Location,
        player: Player,
        setting: RoleSetting,
        checkAgainst: Boolean
    ): Boolean {
        val region: Region = module!!.regionAt(location) ?: return false

        val group = region.regionGroup(module!!)
        return group!!.getRole(player.uniqueId)!!.getSetting(setting) == checkAgainst
    }

    fun checkSettingAt(
        block: Block,
        player: Player,
        setting: RoleSetting,
        checkAgainst: Boolean
    ): Boolean {
        val region: Region = module!!.regionAt(block) ?: return false

        val group = region.regionGroup(module!!)
        return group!!.getRole(player.uniqueId)!!.getSetting(setting) == checkAgainst
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPortalActivate(event: PortalActivateEvent) {
        if (event.player == null) {
            // Activated by redstone -> Always allow. It's the job of the region
            // owner to prevent redstone interactions if a portal shouldn't be activated.
            return
        }

        if (checkSettingAt(event.portal!!.spawn(), event.player!!, RoleSetting.PORTAL, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPortalDeactivate(event: PortalDeactivateEvent) {
        if (checkSettingAt(event.portal!!.spawn(), event.player!!, RoleSetting.PORTAL, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPortalConstruct(event: PortalConstructEvent) {
        // We have to check all blocks here, because otherwise players
        // could "steal" boundary blocks from unowned regions
        for (block in event.boundary!!.allBlocks()) {
            // Portals in regions may only be constructed by region administrators
            if (checkSettingAt(block, event.player!!, RoleSetting.ADMIN, false)) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPortalDestroy(event: PortalDestroyEvent) {
        if (event.portal.owner() == event.player.uniqueId) {
            // Owner may always use their portals
            return
        }

        // We do NOT have to check all blocks here, because
        // an existing portal with its spawn inside a region
        // that the player controls can be considered proof of authority.
        if (checkSettingAt(event.portal.spawn(), event.player, RoleSetting.ADMIN, false)) {
            // Portals in regions may only be destroyed by region administrators
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    fun onPortalLinkConsole(event: PortalLinkConsoleEvent) {
        if (event.portal != null && event.portal!!.owner() == event.player.uniqueId) {
            // Owner may always use their portals
            return
        }

        if (event.portal != null && module!!.regionAt(event.portal!!.spawn()) != null) {
            // Portals in regions may be administrated by region administrators,
            // not only be the owner
            event.setCancelIfNotOwner(false)
        }

        // Portals in regions may only be administrated by region administrators
        // Check permission on console
        if (checkSettingAt(event.console!!, event.player, RoleSetting.ADMIN, false)) {
            event.isCancelled = true
            return
        }

        // Check permission on portal if any
        if (event.portal != null &&
            checkSettingAt(event.portal!!.spawn(), event.player, RoleSetting.ADMIN, false)
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    fun onPortalUnlinkConsole(event: PortalUnlinkConsoleEvent) {
        if (event.portal.owner() == event.player.uniqueId) {
            // Owner may always use their portals
            return
        }

        if (module!!.regionAt(event.portal.spawn()) != null) {
            // Portals in regions may be administrated by region administrators,
            // not only be the owner
            event.setCancelIfNotOwner(false)
        }

        // Portals in regions may only be administrated by region administrators
        // Check permission on console
        if (checkSettingAt(event.console!!, event.player, RoleSetting.ADMIN, false)) {
            event.isCancelled = true
            return
        }

        // Check permission on portal
        if (checkSettingAt(event.portal.spawn(), event.player, RoleSetting.ADMIN, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPortalOpenConsole(event: PortalOpenConsoleEvent) {
        if (event.portal!!.owner() == event.player!!.uniqueId) {
            // Owner may always use their portals
            return
        }

        // Check permission on console
        if (checkSettingAt(event.console!!, event.player!!, RoleSetting.PORTAL, false)) {
            event.isCancelled = true
            return
        }

        // Check permission on portal
        if (checkSettingAt(event.portal!!.spawn(), event.player!!, RoleSetting.PORTAL, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPortalSelectTarget(event: PortalSelectTargetEvent) {
        if (event.portal!!.owner() == event.player!!.uniqueId) {
            // Owner may always use their portals
            return
        }

        // Check permission on source portal
        if (checkSettingAt(event.portal!!.spawn(), event.player!!, RoleSetting.PORTAL, false)) {
            event.isCancelled = true
            return
        }

        // Check permission on target portal
        if (event.target != null &&
            checkSettingAt(event.target!!.spawn(), event.player!!, RoleSetting.PORTAL, false)
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    fun onPortalChangeSettings(event: PortalChangeSettingsEvent) {
        if (event.portal.owner() == event.player.uniqueId) {
            // Owner may always use their portals
            return
        }

        if (module!!.regionAt(event.portal.spawn()) == null) {
            return
        }

        // Portals in regions may be administrated by region administrators,
        // not only be the owner
        event.setCancelIfNotOwner(false)

        // Now check if the player has the permission
        if (checkSettingAt(event.portal.spawn(), event.player, RoleSetting.ADMIN, false)) {
            event.isCancelled = true
        }
    }
}
