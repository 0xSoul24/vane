package org.oddlama.vane.portals

import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.BlueMapWorld
import de.bluecolored.bluemap.api.markers.HtmlMarker
import de.bluecolored.bluemap.api.markers.MarkerSet
import org.bukkit.World
import org.oddlama.vane.external.apache.commons.text.StringEscapeUtils
import org.oddlama.vane.portals.portal.Portal
import java.util.*
import java.util.function.Consumer

class PortalBlueMapLayerDelegate(private val parent: PortalBlueMapLayer) {
    private var bluemapEnabled = false

    val module: Portals?
        get() = parent.module

    fun onEnable() {
        BlueMapAPI.onEnable { api: BlueMapAPI? ->
            module!!.log.info("Enabling BlueMap integration")
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

        module!!.log.info("Disabling BlueMap integration")
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

        api
            .getWorld(world)
            .ifPresent(Consumer { bmWorld: BlueMapWorld? ->
                for (map in bmWorld!!.maps) {
                    map.markerSets[MARKER_SET_ID] = markerSet
                }
            })

        markerSets[world.uid] = markerSet
    }

    fun updateMarker(portal: Portal) {
        removeMarker(portal.id())

        // Don't show private portals
        if (portal.visibility() == Portal.Visibility.PRIVATE) {
            return
        }

        val loc = portal.spawn()
        val marker = HtmlMarker.builder()
            .position(loc.x, loc.y, loc.z)
            .label("Portal " + portal.name())
            .html(parent.langMarkerLabel!!.str(StringEscapeUtils.escapeHtml4(portal.name())))
            .build()

        // Existing markers will be overwritten.
        val pid = portal.id()?.toString() ?: return
        markerSets[loc.world.uid]!!.markers[pid] = marker
    }

    fun removeMarker(portalId: UUID?) {
        for (markerSet in markerSets.values) {
            markerSet.markers.remove(portalId?.toString())
        }
    }

    fun updateAllMarkers() {
        for (portal in module!!.allAvailablePortals().filterNotNull()) {
            // Don't show private portals
            if (portal.visibility() == Portal.Visibility.PRIVATE) {
                continue
            }

            updateMarker(portal)
        }
    }

    companion object {
        const val MARKER_SET_ID: String = "vane_portals.portals"
    }
}