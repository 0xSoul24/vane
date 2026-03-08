package org.oddlama.vane.regions

import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.BlueMapWorld
import de.bluecolored.bluemap.api.markers.ExtrudeMarker
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.bluecolored.bluemap.api.math.Color
import de.bluecolored.bluemap.api.math.Shape
import org.bukkit.World
import org.bukkit.plugin.Plugin
import org.oddlama.vane.regions.region.Region
import java.util.*
import java.util.function.Consumer

class RegionBlueMapLayerDelegate(private val parent: RegionBlueMapLayer) {
    private var bluemapEnabled = false

    val module: Regions
        get() = parent.module!!

    fun onEnable(@Suppress("UNUSED_PARAMETER") plugin: Plugin?) {
        BlueMapAPI.onEnable { api: BlueMapAPI? ->
            this.module.log.info("Enabling BlueMap integration")
            bluemapEnabled = true

            // Create marker sets
            for (world in this.module.server.worlds) {
                createMarkerSet(api!!, world)
            }
            updateAllMarkers()
        }
    }

    fun onDisable() {
        if (!bluemapEnabled) {
            return
        }

        this.module.log.info("Disabling BlueMap integration")
        bluemapEnabled = false
    }

    // worldId -> MarkerSet
    private val markerSets = HashMap<UUID?, MarkerSet>()

    private fun createMarkerSet(api: BlueMapAPI, world: World) {
        if (markerSets.containsKey(world.uid)) {
            return
        }

        val markerSet = MarkerSet.builder()
            .label(parent.langLayerLabel?.str() ?: "")
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

        markerSets[world.uid] = markerSet!!
    }

    fun updateMarker(region: Region) {
        removeMarker(region.id()!!)
        val min = region.extent()!!.min()
        val max = region.extent()!!.max()
        val shape = Shape.createRect(
            min!!.x.toDouble(),
            min.z.toDouble(),
            (max!!.x + 1).toDouble(),
            (max.z + 1).toDouble()
        )

        val marker = ExtrudeMarker.builder()
            .shape(shape, min.y.toFloat(), (max.y + 1).toFloat())
            .label(parent.langMarkerLabel?.str(region.name()) ?: region.name())
            .lineWidth(parent.configLineWidth)
            .lineColor(Color(parent.configLineColor, parent.configLineOpacity.toFloat()))
            .fillColor(Color(parent.configFillColor, parent.configFillOpacity.toFloat()))
            .depthTestEnabled(parent.configDepthTest)
            .centerPosition()
            .build()

        // Existing markers will be overwritten.
        markerSets[min.world.uid]!!.markers[region.id().toString()] = marker
    }

    fun removeMarker(regionId: UUID) {
        for (markerSet in markerSets.values) {
            markerSet.markers.remove(regionId.toString())
        }
    }

    fun updateAllMarkers() {
        for (region in this.module.allRegions()) {
            val r = region ?: continue
            updateMarker(r)
        }
    }

    companion object {
        const val MARKER_SET_ID: String = "vane_regions.regions"
    }
}
