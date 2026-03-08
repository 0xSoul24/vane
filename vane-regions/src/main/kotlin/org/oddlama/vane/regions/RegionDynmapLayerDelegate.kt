package org.oddlama.vane.regions

import org.bukkit.plugin.Plugin
import org.dynmap.DynmapCommonAPI
import org.dynmap.DynmapCommonAPIListener
import org.dynmap.markers.Marker
import org.dynmap.markers.MarkerAPI
import org.dynmap.markers.MarkerSet
import org.oddlama.vane.regions.region.Region
import java.util.*
import java.util.logging.Level

class RegionDynmapLayerDelegate(private val parent: RegionDynmapLayer) {
    private var dynmapApi: DynmapCommonAPI? = null
    private var markerApi: MarkerAPI? = null
    private var dynmapEnabled = false

    private var markerSet: MarkerSet? = null

    val module: Regions
        get() = parent.module!!

    fun onEnable(@Suppress("UNUSED_PARAMETER") plugin: Plugin?) {
        try {
            DynmapCommonAPIListener.register(
                object : DynmapCommonAPIListener() {
                    override fun apiEnabled(api: DynmapCommonAPI?) {
                        dynmapApi = api
                        markerApi = dynmapApi!!.markerAPI
                    }
                }
            )
        } catch (e: Exception) {
            this.module.log.log(Level.WARNING, "Error while enabling dynmap integration!", e)
            return
        }

        if (markerApi == null) {
            return
        }

        this.module.log.info("Enabling dynmap integration")
        dynmapEnabled = true
        createOrLoadLayer()
    }

    fun onDisable() {
        if (!dynmapEnabled) {
            return
        }

        this.module.log.info("Disabling dynmap integration")
        dynmapEnabled = false
        dynmapApi = null
        markerApi = null
    }

    private fun createOrLoadLayer() {
        // Create or retrieve layer
        markerSet = markerApi!!.getMarkerSet(RegionDynmapLayer.LAYER_ID)
        if (markerSet == null) {
            markerSet = markerApi!!.createMarkerSet(
                RegionDynmapLayer.LAYER_ID,
                // Use safe call with fallback for nullable TranslatedMessage
                parent.langLayerLabel?.str() ?: "",
                null,
                false
            )
        }

        if (markerSet == null) {
            this.module.log.severe("Failed to create dynmap region marker set!")
            return
        }

        // Update attributes
        markerSet!!.markerSetLabel = parent.langLayerLabel?.str() ?: ""
        markerSet!!.layerPriority = parent.configLayerPriority
        markerSet!!.hideByDefault = parent.configLayerHide

        // Initial update
        updateAllMarkers()
    }

    private fun idFor(regionId: UUID): String {
        return regionId.toString()
    }

    private fun idFor(region: Region): String {
        return idFor(region.id()!!)
    }

    fun updateMarker(region: Region) {
        if (!dynmapEnabled) {
            return
        }

        // Area markers can't be updated.
        removeMarker(region.id()!!)

        val min = region.extent()!!.min()
        val max = region.extent()!!.max()
        val worldName = min!!.world.name
        val markerId = idFor(region)
        // Use safe call with fallback for nullable TranslatedMessage
        val markerLabel = parent.langMarkerLabel?.str(region.name()) ?: region.name()

        val xs = doubleArrayOf(min.x.toDouble(), (max!!.x + 1).toDouble())
        val zs = doubleArrayOf(min.z.toDouble(), (max.z + 1).toDouble())
        val area = markerSet!!.createAreaMarker(markerId, markerLabel, false, worldName, xs, zs, false)
        area.setRangeY((max.y + 1).toDouble(), min.y.toDouble())
        area.setLineStyle(parent.configLineWeight, parent.configLineOpacity, parent.configLineColor)
        area.setFillStyle(parent.configFillOpacity, parent.configFillColor)
    }

    fun removeMarker(regionId: UUID) {
        removeMarker(idFor(regionId))
    }

    fun removeMarker(markerId: String?) {
        if (!dynmapEnabled || markerId == null) {
            return
        }

        removeMarker(markerSet!!.findMarker(markerId))
    }

    fun removeMarker(marker: Marker?) {
        if (!dynmapEnabled || marker == null) {
            return
        }

        marker.deleteMarker()
    }

    fun updateAllMarkers() {
        if (!dynmapEnabled) {
            return
        }

        // Update all existing
        val idSet = HashSet<String?>()
        for (region in this.module.allRegions()) {
            val r = region ?: continue
            idSet.add(idFor(r))
            updateMarker(r)
        }

        // Remove orphaned
        for (marker in markerSet!!.markers) {
            val id = marker.markerID
            if (id != null && !idSet.contains(id)) {
                removeMarker(marker)
            }
        }
    }
}
