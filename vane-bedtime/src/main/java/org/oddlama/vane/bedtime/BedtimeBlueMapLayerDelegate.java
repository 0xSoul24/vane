package org.oddlama.vane.bedtime;

import static org.oddlama.vane.external.apache.commons.text.StringEscapeUtils.escapeHtml4;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.HtmlMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public class BedtimeBlueMapLayerDelegate {

    public static final String MARKER_SET_ID = "vane_bedtime.bedtime";

    private final BedtimeBlueMapLayer parent;

    private boolean bluemapEnabled = false;

    public BedtimeBlueMapLayerDelegate(final BedtimeBlueMapLayer parent) {
        this.parent = parent;
    }

    public Bedtime getModule() {
        return parent.getModule();
    }

    public void onEnable(final Plugin plugin) {
        BlueMapAPI.onEnable(api -> {
            getModule().getLog().info("Enabling BlueMap integration");
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

        getModule().getLog().info("Disabling BlueMap integration");
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

    public void updateMarker(final OfflinePlayer player) {
        removeMarker(player.getUniqueId());
        final var loc = player.getRespawnLocation();
        if (loc == null) {
            return;
        }

        final var marker = HtmlMarker.builder()
            .position(loc.getX(), loc.getY(), loc.getZ())
            .label("Bed for " + player.getName())
            .html(parent.langMarkerLabel.str(escapeHtml4(player.getName())))
            .build();

        // Existing markers will be overwritten.
        markerSets.get(loc.getWorld().getUID()).getMarkers().put(player.getUniqueId().toString(), marker);
    }

    public void removeMarker(final UUID playerId) {
        for (final var markerSet : markerSets.values()) {
            markerSet.getMarkers().remove(playerId.toString());
        }
    }

    public void updateAllMarkers() {
        // Update all existing
        for (final var player : getModule().getOfflinePlayersWithValidName()) {
            updateMarker(player);
        }
    }
}