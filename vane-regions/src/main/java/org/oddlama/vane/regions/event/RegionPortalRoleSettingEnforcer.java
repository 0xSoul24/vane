package org.oddlama.vane.regions.event;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.portals.event.PortalActivateEvent;
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent;
import org.oddlama.vane.portals.event.PortalConstructEvent;
import org.oddlama.vane.portals.event.PortalDeactivateEvent;
import org.oddlama.vane.portals.event.PortalDestroyEvent;
import org.oddlama.vane.portals.event.PortalLinkConsoleEvent;
import org.oddlama.vane.portals.event.PortalOpenConsoleEvent;
import org.oddlama.vane.portals.event.PortalSelectTargetEvent;
import org.oddlama.vane.portals.event.PortalUnlinkConsoleEvent;
import org.oddlama.vane.regions.Regions;
import org.oddlama.vane.regions.region.RoleSetting;

public class RegionPortalRoleSettingEnforcer extends Listener<Regions> {

    public RegionPortalRoleSettingEnforcer(Context<Regions> context) {
        super(context);
    }

    public boolean checkSettingAt(
        final Location location,
        final Player player,
        final RoleSetting setting,
        final boolean checkAgainst
    ) {
        final var region = getModule().regionAt(location);
        if (region == null) {
            return false;
        }

        final var group = region.regionGroup(getModule());
        return group.getRole(player.getUniqueId()).getSetting(setting) == checkAgainst;
    }

    public boolean checkSettingAt(
        final Block block,
        final Player player,
        final RoleSetting setting,
        final boolean checkAgainst
    ) {
        final var region = getModule().regionAt(block);
        if (region == null) {
            return false;
        }

        final var group = region.regionGroup(getModule());
        return group.getRole(player.getUniqueId()).getSetting(setting) == checkAgainst;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPortalActivate(final PortalActivateEvent event) {
        if (event.getPlayer() == null) {
            // Activated by redstone -> Always allow. It's the job of the region
            // owner to prevent redstone interactions if a portal shouldn't be activated.
            return;
        }

        if (checkSettingAt(event.getPortal().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPortalDeactivate(final PortalDeactivateEvent event) {
        if (checkSettingAt(event.getPortal().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPortalConstruct(final PortalConstructEvent event) {
        // We have to check all blocks here, because otherwise players
        // could "steal" boundary blocks from unowned regions
        for (final var block : event.getBoundary().allBlocks()) {
            // Portals in regions may only be constructed by region administrators
            if (checkSettingAt(block, event.getPlayer(), RoleSetting.ADMIN, false)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPortalDestroy(final PortalDestroyEvent event) {
        if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
            // Owner may always use their portals
            return;
        }

        // We do NOT have to check all blocks here, because
        // an existing portal with its spawn inside a region
        // that the player controls can be considered proof of authority.
        if (checkSettingAt(event.getPortal().spawn(), event.getPlayer(), RoleSetting.ADMIN, false)) {
            // Portals in regions may only be destroyed by region administrators
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onPortalLinkConsole(final PortalLinkConsoleEvent event) {
        if (event.getPortal() != null && event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
            // Owner may always use their portals
            return;
        }

        if (event.getPortal() != null && getModule().regionAt(event.getPortal().spawn()) != null) {
            // Portals in regions may be administrated by region administrators,
            // not only be the owner
            event.setCancelIfNotOwner(false);
        }

        // Portals in regions may only be administrated by region administrators
        // Check permission on console
        if (checkSettingAt(event.getConsole(), event.getPlayer(), RoleSetting.ADMIN, false)) {
            event.setCancelled(true);
            return;
        }

        // Check permission on portal if any
        if (
            event.getPortal() != null &&
            checkSettingAt(event.getPortal().spawn(), event.getPlayer(), RoleSetting.ADMIN, false)
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onPortalUnlinkConsole(final PortalUnlinkConsoleEvent event) {
        if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
            // Owner may always use their portals
            return;
        }

        if (getModule().regionAt(event.getPortal().spawn()) != null) {
            // Portals in regions may be administrated by region administrators,
            // not only be the owner
            event.setCancelIfNotOwner(false);
        }

        // Portals in regions may only be administrated by region administrators
        // Check permission on console
        if (checkSettingAt(event.getConsole(), event.getPlayer(), RoleSetting.ADMIN, false)) {
            event.setCancelled(true);
            return;
        }

        // Check permission on portal
        if (checkSettingAt(event.getPortal().spawn(), event.getPlayer(), RoleSetting.ADMIN, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPortalOpenConsole(final PortalOpenConsoleEvent event) {
        if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
            // Owner may always use their portals
            return;
        }

        // Check permission on console
        if (checkSettingAt(event.getConsole(), event.getPlayer(), RoleSetting.PORTAL, false)) {
            event.setCancelled(true);
            return;
        }

        // Check permission on portal
        if (checkSettingAt(event.getPortal().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPortalSelectTarget(final PortalSelectTargetEvent event) {
        if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
            // Owner may always use their portals
            return;
        }

        // Check permission on source portal
        if (checkSettingAt(event.getPortal().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
            event.setCancelled(true);
            return;
        }

        // Check permission on target portal
        if (
            event.getTarget() != null &&
            checkSettingAt(event.getTarget().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onPortalChangeSettings(final PortalChangeSettingsEvent event) {
        if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
            // Owner may always use their portals
            return;
        }

        if (getModule().regionAt(event.getPortal().spawn()) == null) {
            return;
        }

        // Portals in regions may be administrated by region administrators,
        // not only be the owner
        event.setCancelIfNotOwner(false);

        // Now check if the player has the permission
        if (checkSettingAt(event.getPortal().spawn(), event.getPlayer(), RoleSetting.ADMIN, false)) {
            event.setCancelled(true);
        }
    }
}
