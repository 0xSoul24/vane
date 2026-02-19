package org.oddlama.vane.regions;

import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.oddlama.vane.regions.region.Region;

public class RegionDynmapLayerDelegate {

    private final RegionDynmapLayer parent;

    private DynmapCommonAPI dynmapApi = null;
    private MarkerAPI markerApi = null;
    private boolean dynmapEnabled = false;

    private MarkerSet markerSet = null;

    public RegionDynmapLayerDelegate(final RegionDynmapLayer parent) {
        this.parent = parent;
    }

    public Regions getModule() {
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
        markerSet = markerApi.getMarkerSet(RegionDynmapLayer.LAYER_ID);
        if (markerSet == null) {
            markerSet = markerApi.createMarkerSet(
                RegionDynmapLayer.LAYER_ID,
                parent.langLayerLabel.str(),
                null,
                false
            );
        }

        if (markerSet == null) {
            getModule().log.severe("Failed to create dynmap region marker set!");
            return;
        }

        // Update attributes
        markerSet.setMarkerSetLabel(parent.langLayerLabel.str());
        markerSet.setLayerPriority(parent.configLayerPriority);
        markerSet.setHideByDefault(parent.configLayerHide);

        // Initial update
        updateAllMarkers();
    }

    private String idFor(final UUID regionId) {
        return regionId.toString();
    }

    private String idFor(final Region region) {
        return idFor(region.id());
    }

    public void updateMarker(final Region region) {
        if (!dynmapEnabled) {
            return;
        }

        // Area markers can't be updated.
        removeMarker(region.id());

        final var min = region.extent().min();
        final var max = region.extent().max();
        final var worldName = min.getWorld().getName();
        final var markerId = idFor(region);
        final var markerLabel = parent.langMarkerLabel.str(region.name());

        final var xs = new double[] { min.getX(), max.getX() + 1 };
        final var zs = new double[] { min.getZ(), max.getZ() + 1 };
        final var area = markerSet.createAreaMarker(markerId, markerLabel, false, worldName, xs, zs, false);
        area.setRangeY(max.getY() + 1, min.getY());
        area.setLineStyle(parent.configLineWeight, parent.configLineOpacity, parent.configLineColor);
        area.setFillStyle(parent.configFillOpacity, parent.configFillColor);
    }

    public void removeMarker(final UUID regionId) {
        removeMarker(idFor(regionId));
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
        for (final var region : getModule().allRegions()) {
            idSet.add(idFor(region));
            updateMarker(region);
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
