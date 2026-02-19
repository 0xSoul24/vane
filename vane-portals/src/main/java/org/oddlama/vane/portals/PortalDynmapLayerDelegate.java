package org.oddlama.vane.portals;

import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;
import org.oddlama.vane.portals.portal.Portal;

public class PortalDynmapLayerDelegate {

    private final PortalDynmapLayer parent;

    private DynmapCommonAPI dynmapApi = null;
    private MarkerAPI markerApi = null;
    private boolean dynmapEnabled = false;

    private MarkerSet markerSet = null;
    private MarkerIcon markerIcon = null;

    public PortalDynmapLayerDelegate(final PortalDynmapLayer parent) {
        this.parent = parent;
    }

    public Portals getModule() {
        return parent.getModule();
    }

    public void onEnable(final Plugin plugin) {
        try {
            DynmapCommonAPIListener.register(
                new DynmapCommonAPIListener() {
                    @Override
                    public void apiEnabled(DynmapCommonAPI api) {
                        dynmapApi = api;
                        markerApi = dynmapApi.getMarkerAPI();
                    }
                }
            );
        } catch (Exception e) {
            getModule().log.log(Level.WARNING, "Error while enabling dynmap integration!", e);
            return;
        }

        if (markerApi == null) {
            return;
        }

        getModule().log.info("Enabling dynmap integration");
        dynmapEnabled = true;
        createOrLoadLayer();
    }

    public void onDisable() {
        if (!dynmapEnabled) {
            return;
        }

        getModule().log.info("Disabling dynmap integration");
        dynmapEnabled = false;
        dynmapApi = null;
        markerApi = null;
    }

    private void createOrLoadLayer() {
        // Create or retrieve layer
        markerSet = markerApi.getMarkerSet(PortalDynmapLayer.LAYER_ID);
        if (markerSet == null) {
            markerSet = markerApi.createMarkerSet(
                PortalDynmapLayer.LAYER_ID,
                parent.langLayerLabel.str(),
                null,
                false
            );
        }

        if (markerSet == null) {
            getModule().log.severe("Failed to create dynmap portal marker set!");
            return;
        }

        // Update attributes
        markerSet.setMarkerSetLabel(parent.langLayerLabel.str());
        markerSet.setLayerPriority(parent.configLayerPriority);
        markerSet.setHideByDefault(parent.configLayerHide);

        // Load marker
        markerIcon = markerApi.getMarkerIcon(parent.configMarkerIcon);
        if (markerIcon == null) {
            getModule().log.severe("Failed to load dynmap portal marker icon!");
            return;
        }

        // Initial update
        updateAllMarkers();
    }

    private String idFor(final UUID portalId) {
        return portalId.toString();
    }

    private String idFor(final Portal portal) {
        return idFor(portal.id());
    }

    public void updateMarker(final Portal portal) {
        if (!dynmapEnabled) {
            return;
        }

        // Don't show private portals
        if (portal.visibility() == Portal.Visibility.PRIVATE) {
            removeMarker(portal.id());
            return;
        }

        final var loc = portal.spawn();
        final var worldName = loc.getWorld().getName();
        final var markerId = idFor(portal);
        final var markerLabel = parent.langMarkerLabel.str(portal.name());

        markerSet.createMarker(
            markerId,
            markerLabel,
            worldName,
            loc.getX(),
            loc.getY(),
            loc.getZ(),
                markerIcon,
            false
        );
    }

    public void removeMarker(final UUID portalId) {
        removeMarker(idFor(portalId));
    }

    public void removeMarker(final String markerId) {
        if (!dynmapEnabled || markerId == null) {
            return;
        }

        removeMarker(markerSet.findMarker(markerId));
    }

    public void removeMarker(final Marker marker) {
        if (!dynmapEnabled || marker == null) {
            return;
        }

        marker.deleteMarker();
    }

    public void updateAllMarkers() {
        if (!dynmapEnabled) {
            return;
        }

        // Update all existing
        final var idSet = new HashSet<String>();
        for (final var portal : getModule().allAvailablePortals()) {
            idSet.add(idFor(portal));
            updateMarker(portal);
        }

        // Remove orphaned
        for (final var marker : markerSet.getMarkers()) {
            final var id = marker.getMarkerID();
            if (id != null && !idSet.contains(id)) {
                removeMarker(marker);
            }
        }
    }
}
