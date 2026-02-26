package org.oddlama.vane.bedtime;

import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

public class BedtimeDynmapLayerDelegate {

    private final BedtimeDynmapLayer parent;

    private DynmapCommonAPI dynmapApi = null;
    private MarkerAPI markerApi = null;
    private boolean dynmapEnabled = false;
    private MarkerSet markerSet = null;
    private MarkerIcon markerIcon = null;

    public BedtimeDynmapLayerDelegate(final BedtimeDynmapLayer parent) {
        this.parent = parent;
    }

    public Bedtime getModule() {
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
            getModule().getLog().log(Level.WARNING, "Error while enabling dynmap integration!", e);
            return;
        }

        if (markerApi == null) {
            return;
        }

        getModule().getLog().info("Enabling dynmap integration");
        dynmapEnabled = true;
        createOrLoadLayer();
    }

    public void onDisable() {
        if (!dynmapEnabled) {
            return;
        }

        getModule().getLog().info("Disabling dynmap integration");
        dynmapEnabled = false;
        dynmapApi = null;
        markerApi = null;
    }

    private void createOrLoadLayer() {
        // Create or retrieve layer
        markerSet = markerApi.getMarkerSet(BedtimeDynmapLayer.LAYER_ID);
        if (markerSet == null) {
            markerSet = markerApi.createMarkerSet(
                BedtimeDynmapLayer.LAYER_ID,
                parent.langLayerLabel.str(),
                null,
                false
            );
        }

        if (markerSet == null) {
            getModule().getLog().severe("Failed to create dynmap bedtime marker set!");
            return;
        }

        // Update attributes
        markerSet.setMarkerSetLabel(parent.langLayerLabel.str());
        markerSet.setLayerPriority(parent.configLayerPriority);
        markerSet.setHideByDefault(parent.configLayerHide);

        // Load marker
        markerIcon = markerApi.getMarkerIcon(parent.configMarkerIcon);
        if (markerIcon == null) {
            getModule().getLog().severe("Failed to load dynmap bedtime marker icon!");
            return;
        }

        // Initial update
        updateAllMarkers();
    }

    private String idFor(final UUID playerId) {
        return playerId.toString();
    }

    private String idFor(final OfflinePlayer player) {
        return idFor(player.getUniqueId());
    }

    public boolean updateMarker(final OfflinePlayer player) {
        if (!dynmapEnabled) {
            return false;
        }

        final var loc = player.getRespawnLocation();
        if (loc == null) {
            return false;
        }

        final var worldName = loc.getWorld().getName();
        final var markerId = idFor(player);
        final var markerLabel = parent.langMarkerLabel.str(player.getName());

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
        return true;
    }

    public void removeMarker(final UUID playerId) {
        removeMarker(idFor(playerId));
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
        for (final var player : getModule().getOfflinePlayersWithValidName()) {
            if (updateMarker(player)) {
                idSet.add(idFor(player));
            }
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
