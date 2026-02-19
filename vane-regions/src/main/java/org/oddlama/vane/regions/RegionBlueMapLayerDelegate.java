package org.oddlama.vane.regions;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.oddlama.vane.regions.region.Region;

public class RegionBlueMapLayerDelegate {

    public static final String MARKER_SET_ID = "vane_regions.regions";

    private final RegionBlueMapLayer parent;

    private boolean bluemapEnabled = false;

    public RegionBlueMapLayerDelegate(final RegionBlueMapLayer parent) {
        this.parent = parent;
    }

    public Regions getModule() {
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

    public void updateMarker(final Region region) {
        removeMarker(region.id());
        final var min = region.extent().min();
        final var max = region.extent().max();
        final var shape = Shape.createRect(min.getX(), min.getZ(), max.getX() + 1, max.getZ() + 1);

        final var marker = ExtrudeMarker.builder()
            .shape(shape, min.getY(), max.getY() + 1)
            .label(parent.langMarkerLabel.str(region.name()))
            .lineWidth(parent.configLineWidth)
            .lineColor(new Color(parent.configLineColor, (float) parent.configLineOpacity))
            .fillColor(new Color(parent.configFillColor, (float) parent.configFillOpacity))
            .depthTestEnabled(parent.configDepthTest)
            .centerPosition()
            .build();

        // Existing markers will be overwritten.
        markerSets.get(min.getWorld().getUID()).getMarkers().put(region.id().toString(), marker);
    }

    public void removeMarker(final UUID regionId) {
        for (final var markerSet : markerSets.values()) {
            markerSet.getMarkers().remove(regionId.toString());
        }
    }

    public void updateAllMarkers() {
        for (final var region : getModule().allRegions()) {
            updateMarker(region);
        }
    }
}
