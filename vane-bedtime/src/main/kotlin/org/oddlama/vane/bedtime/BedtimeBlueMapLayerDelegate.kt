package org.oddlama.vane.bedtime

import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.BlueMapWorld
import de.bluecolored.bluemap.api.markers.HtmlMarker
import de.bluecolored.bluemap.api.markers.MarkerSet
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.plugin.Plugin
import org.oddlama.vane.external.apache.commons.text.StringEscapeUtils
import java.util.*

class BedtimeBlueMapLayerDelegate(private val parent: BedtimeBlueMapLayer) {
    private var bluemapEnabled = false

    val module: Bedtime?
        get() = parent.module

    fun onEnable(plugin: Plugin?) {
        BlueMapAPI.onEnable { api: BlueMapAPI? ->
            module?.log?.info("Enabling BlueMap integration")
            bluemapEnabled = true

            // Create marker sets
            for (world in module!!.server.worlds) {
                createMarkerSet(api!!, world)
            }
            updateAllMarkers()
        }
    }

    fun onDisable() {
        if (!bluemapEnabled) {
            return
        }

        module?.log?.info("Disabling BlueMap integration")
        bluemapEnabled = false
    }

    // worldId -> MarkerSet
    private val markerSets = HashMap<UUID?, MarkerSet>()

    private fun createMarkerSet(api: BlueMapAPI, world: World) {
        if (markerSets.containsKey(world.uid)) {
            return
        }

        val markerSet = MarkerSet.builder()
            .label(parent.langLayerLabel!!.str())
            .toggleable(true)
            .defaultHidden(parent.configHideByDefault)
            .build()

        api.getWorld(world).ifPresent { bmWorld: BlueMapWorld? ->
            if (bmWorld != null) {
                for (map in bmWorld.maps) {
                    map.markerSets[MARKER_SET_ID] = markerSet
                }
            }
        }

        markerSets[world.uid] = markerSet
    }

    fun updateMarker(player: OfflinePlayer) {
        removeMarker(player.uniqueId)
        val loc = player.respawnLocation ?: return

        val marker = HtmlMarker.builder()
            .position(loc.x, loc.y, loc.z)
            .label("Bed for " + player.name)
            .html(parent.langMarkerLabel!!.str(StringEscapeUtils.escapeHtml4(player.name)))
            .build()

        // Existing markers will be overwritten.
        markerSets[loc.world.uid]!!.markers[player.uniqueId.toString()] = marker
    }

    fun removeMarker(playerId: UUID) {
        for (markerSet in markerSets.values) {
            markerSet.markers.remove(playerId.toString())
        }
    }

    fun updateAllMarkers() {
        // Update all existing
        for (player in module!!.offlinePlayersWithValidName) {
            updateMarker(player)
        }
    }

    companion object {
        const val MARKER_SET_ID: String = "vane_bedtime.bedtime"
    }
}