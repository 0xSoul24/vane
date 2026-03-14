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

/**
 * Low-level dynmap API delegate used by `RegionDynmapLayer`.
 */
class RegionDynmapLayerDelegate(private val parent: RegionDynmapLayer) {
    /**
     * Active dynmap API instance when integration is enabled.
     */
    private var dynmapApi: DynmapCommonAPI? = null
    /**
     * Dynmap marker API handle.
     */
    private var markerApi: MarkerAPI? = null
    /**
     * Whether dynmap integration is currently active.
     */
    private var dynmapEnabled = false

    /**
     * Marker set used for all region overlays.
     */
    private var markerSet: MarkerSet? = null

    /**
     * Owning regions module instance.
     */
    private val module: Regions
        get() = requireNotNull(parent.module)

    /**
     * Registers dynmap listeners and creates/loads the region marker layer.
     */
    fun onEnable(@Suppress("UNUSED_PARAMETER") plugin: Plugin?) {
        try {
            DynmapCommonAPIListener.register(
                object : DynmapCommonAPIListener() {
                    override fun apiEnabled(api: DynmapCommonAPI?) {
                        dynmapApi = api
                        markerApi = dynmapApi?.markerAPI
                    }
                }
            )
        } catch (e: Exception) {
            module.log.log(Level.WARNING, "Error while enabling dynmap integration!", e)
            return
        }

        if (markerApi == null) return

        module.log.info("Enabling dynmap integration")
        dynmapEnabled = true
        createOrLoadLayer()
    }

    /**
     * Disables dynmap integration state.
     */
    fun onDisable() {
        if (!dynmapEnabled) {
            return
        }

        module.log.info("Disabling dynmap integration")
        dynmapEnabled = false
        dynmapApi = null
        markerApi = null
    }

    /**
     * Creates or updates the dynmap marker set and applies current style settings.
     */
    private fun createOrLoadLayer() {
        val api = markerApi ?: return

        // Create or retrieve layer
        markerSet = api.getMarkerSet(RegionDynmapLayer.LAYER_ID)
        if (markerSet == null) {
            markerSet = api.createMarkerSet(
                RegionDynmapLayer.LAYER_ID,
                // Use safe call with fallback for nullable TranslatedMessage
                parent.langLayerLabel?.str() ?: "",
                null,
                false
            )
        }

        if (markerSet == null) {
            module.log.severe("Failed to create dynmap region marker set!")
            return
        }

        // Update attributes
        markerSet!!.markerSetLabel = parent.langLayerLabel?.str() ?: ""
        markerSet!!.layerPriority = parent.configLayerPriority
        markerSet!!.hideByDefault = parent.configLayerHide

        // Initial update
        updateAllMarkers()
    }

    /**
     * Converts a region UUID to dynmap marker id.
     */
    private fun idFor(regionId: UUID): String {
        return regionId.toString()
    }

    /**
     * Resolves marker id for a region object.
     */
    private fun idFor(region: Region): String? = region.id()?.let(::idFor)

    /**
     * Recreates a dynmap marker for the given region.
     */
    fun updateMarker(region: Region) {
        if (!dynmapEnabled) {
            return
        }

        // Area markers can't be updated.
        val regionId = region.id() ?: return
        val extent = region.extent() ?: return
        removeMarker(regionId)

        val min = extent.min() ?: return
        val max = extent.max() ?: return
        val worldName = min.world.name
        val markerId = idFor(region) ?: return
        // Use safe call with fallback for nullable TranslatedMessage
        val markerLabel = parent.langMarkerLabel?.str(region.name()) ?: region.name()

        val markerSetRef = markerSet ?: return
        val xs = doubleArrayOf(min.x.toDouble(), (max.x + 1).toDouble())
        val zs = doubleArrayOf(min.z.toDouble(), (max.z + 1).toDouble())
        val area = markerSetRef.createAreaMarker(markerId, markerLabel, false, worldName, xs, zs, false)
        area.setRangeY((max.y + 1).toDouble(), min.y.toDouble())
        area.setLineStyle(parent.configLineWeight, parent.configLineOpacity, parent.configLineColor)
        area.setFillStyle(parent.configFillOpacity, parent.configFillColor)
    }

    /**
     * Removes marker by region id.
     */
    fun removeMarker(regionId: UUID) {
        removeMarker(idFor(regionId))
    }

    /**
     * Removes marker by marker id.
     */
    fun removeMarker(markerId: String?) {
        if (!dynmapEnabled || markerId == null) return

        removeMarker(markerSet?.findMarker(markerId))
    }

    /**
     * Removes a concrete marker instance.
     */
    fun removeMarker(marker: Marker?) {
        if (!dynmapEnabled || marker == null) return

        marker.deleteMarker()
    }

    /**
     * Synchronizes all region markers and prunes orphaned entries.
     */
    fun updateAllMarkers() {
        if (!dynmapEnabled) {
            return
        }

        // Update all existing
        val markerSetRef = markerSet ?: return
        val idSet = HashSet<String>()
        for (region in module.allRegions()) {
            val r = region ?: continue
            val regionId = idFor(r) ?: continue
            idSet.add(regionId)
            updateMarker(r)
        }

        // Remove orphaned
        for (marker in markerSetRef.markers) {
            val id = marker.markerID
            if (id != null && !idSet.contains(id)) {
                removeMarker(marker)
            }
        }
    }
}
