package org.oddlama.vane.portals;

import static org.oddlama.vane.external.apache.commons.text.StringEscapeUtils.escapeHtml4;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.HtmlMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.oddlama.vane.portals.portal.Portal;

public class PortalBlueMapLayerDelegate {

    public static final String MARKER_SET_ID = "vane_portals.portals";

    private final PortalBlueMapLayer parent;

    private boolean bluemapEnabled = false;

    public PortalBlueMapLayerDelegate(final PortalBlueMapLayer parent) {
        this.parent = parent;
    }

    public Portals getModule() {
        return parent.getModule();
    }

    public void onEnable(final Plugin plugin) {
        BlueMapAPI.onEnable(api -> {
            getModule().log.info("Enabling BlueMap integration");
            bluemapEnabled = true;

            // Create marker sets
            for (final var world : getModule().getServer().getWorlds()) {
                createMarkerSet(api, world);
            }

            updateAllMarkers();
        });
    }

    public void onDisable() {
        if (!bluemapEnabled) {
            return;
        }

        getModule().log.info("Disabling BlueMap integration");
        bluemapEnabled = false;
    }

    // worldId -> MarkerSet
    private final HashMap<UUID, MarkerSet> markerSets = new HashMap<>();

    private void createMarkerSet(final BlueMapAPI api, final World world) {
        if (markerSets.containsKey(world.getUID())) {
            return;
        }

        final var markerSet = MarkerSet.builder()
            .label(parent.langLayerLabel.str())
            .toggleable(true)
            .defaultHidden(parent.configHideByDefault)
            .build();

        api
            .getWorld(world)
            .ifPresent(bmWorld -> {
                for (final var map : bmWorld.getMaps()) {
                    map.getMarkerSets().put(MARKER_SET_ID, markerSet);
                }
            });

        markerSets.put(world.getUID(), markerSet);
    }

    public void updateMarker(final Portal portal) {
        removeMarker(portal.id());

        // Don't show private portals
        if (portal.visibility() == Portal.Visibility.PRIVATE) {
            return;
        }

        final var loc = portal.spawn();
        final var marker = HtmlMarker.builder()
            .position(loc.getX(), loc.getY(), loc.getZ())
            .label("Portal " + portal.name())
            .html(parent.langMarkerLabel.str(escapeHtml4(portal.name())))
            .build();

        // Existing markers will be overwritten.
        markerSets.get(loc.getWorld().getUID()).getMarkers().put(portal.id().toString(), marker);
    }

    public void removeMarker(final UUID portalId) {
        for (final var markerSet : markerSets.values()) {
            markerSet.getMarkers().remove(portalId.toString());
        }
    }

    public void updateAllMarkers() {
        for (final var portal : getModule().allAvailablePortals()) {
            // Don't show private portals
            if (portal.visibility() == Portal.Visibility.PRIVATE) {
                continue;
            }

            updateMarker(portal);
        }
    }
}