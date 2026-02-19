package org.oddlama.vane.regions;

import org.bukkit.plugin.Plugin;
import org.oddlama.vane.regions.event.RegionPortalRoleSettingEnforcer;
import org.oddlama.vane.regions.region.RoleSetting;

public class RegionPortalIntegration {

    public RegionPortalIntegration(Regions context, Plugin portalsPlugin) {
        new RegionPortalRoleSettingEnforcer(context);

        if (portalsPlugin instanceof final org.oddlama.vane.portals.Portals portals) {
            // Register callback to portals module so portals
            // can find out if two portals are in the same region group
            portals.setIsInSameRegionGroupCallback((a, b) -> {
                final var regA = context.regionAt(a.spawn());
                final var regB = context.regionAt(b.spawn());
                if (regA == null || regB == null) {
                    return regA == regB;
                }
                return regA.regionGroupId().equals(regB.regionGroupId());
            });

            portals.setPlayerCanUsePortalsInRegionGroupOfCallback((player, portal) -> {
                final var region = context.regionAt(portal.spawn());
                if (region == null) {
                    // No region -> no restriction.
                    return true;
                }
                final var group = region.regionGroup(context.getModule());
                return group.getRole(player.getUniqueId()).getSetting(RoleSetting.PORTAL);
            });
        }
    }
}
