package org.oddlama.vane.regions

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.oddlama.vane.portals.Portals
import org.oddlama.vane.portals.portal.Portal
import org.oddlama.vane.regions.event.RegionPortalRoleSettingEnforcer
import org.oddlama.vane.regions.region.RoleSetting

class RegionPortalIntegration(context: Regions, portalsPlugin: Plugin?) {
    init {
        RegionPortalRoleSettingEnforcer(context)

        if (portalsPlugin is Portals) {
            // Register callback to portals module so portals
            // can find out if two portals are in the same region group
            portalsPlugin.setIsInSameRegionGroupCallback { a: Portal?, b: Portal? ->
                val regA = context.regionAt(a!!.spawn())
                val regB = context.regionAt(b!!.spawn())
                if (regA == null || regB == null) {
                    return@setIsInSameRegionGroupCallback regA == regB
                }
                regA.regionGroupId() == regB.regionGroupId()
            }

            portalsPlugin.setPlayerCanUsePortalsInRegionGroupOfCallback { player: Player?, portal: Portal? ->
                val region = context.regionAt(portal!!.spawn()) ?: // No region -> no restriction.
                return@setPlayerCanUsePortalsInRegionGroupOfCallback true
                val group = region.regionGroup(context.module!!)
                group!!.getRole(player!!.uniqueId)!!.getSetting(RoleSetting.PORTAL)
            }
        }
    }
}
