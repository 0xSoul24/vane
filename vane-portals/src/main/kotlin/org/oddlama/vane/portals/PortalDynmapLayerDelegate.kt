package org.oddlama.vane.portals

import org.dynmap.DynmapCommonAPI
import org.dynmap.DynmapCommonAPIListener
import org.dynmap.markers.Marker
import org.dynmap.markers.MarkerAPI
import org.dynmap.markers.MarkerIcon
import org.dynmap.markers.MarkerSet
import org.oddlama.vane.portals.portal.Portal
import java.util.*
import java.util.logging.Level

class PortalDynmapLayerDelegate(private val parent: PortalDynmapLayer) {
    private var dynmapApi: DynmapCommonAPI? = null
    private var markerApi: MarkerAPI? = null
    private var dynmapEnabled = false

    private var markerSet: MarkerSet? = null
    private var markerIcon: MarkerIcon? = null

    val module: Portals?
        get() = parent.module

    fun onEnable() {
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
            module!!.log.log(Level.WARNING, "Error while enabling dynmap integration!", e)
            return
        }

        if (markerApi == null) {
            return
        }

        module!!.log.info("Enabling dynmap integration")
        dynmapEnabled = true
        createOrLoadLayer()
    }

    fun onDisable() {
        if (!dynmapEnabled) {
            return
        }

        module!!.log.info("Disabling dynmap integration")
        dynmapEnabled = false
        dynmapApi = null
        markerApi = null
    }

    private fun createOrLoadLayer() {
        // Create or retrieve layer
        markerSet = markerApi!!.getMarkerSet(PortalDynmapLayer.LAYER_ID)
        if (markerSet == null) {
            markerSet = markerApi!!.createMarkerSet(
                PortalDynmapLayer.LAYER_ID,
                parent.langLayerLabel!!.str(),
                null,
                false
            )
        }

        if (markerSet == null) {
            module!!.log.severe("Failed to create dynmap portal marker set!")
            return
        }

        // Update attributes
        markerSet!!.markerSetLabel = parent.langLayerLabel!!.str()
        markerSet!!.layerPriority = parent.configLayerPriority
        markerSet!!.hideByDefault = parent.configLayerHide

        // Load marker
        markerIcon = markerApi!!.getMarkerIcon(parent.configMarkerIcon)
        if (markerIcon == null) {
            module!!.log.severe("Failed to load dynmap portal marker icon!")
            return
        }

        // Initial update
        updateAllMarkers()
    }

    private fun idFor(portalId: UUID?): String? {
        return portalId?.toString()
    }

    private fun idFor(portal: Portal): String? {
        return idFor(portal.id())
    }

    fun updateMarker(portal: Portal) {
        if (!dynmapEnabled) {
            return
        }

        // Don't show private portals
        if (portal.visibility() == Portal.Visibility.PRIVATE) {
            removeMarker(portal.id())
            return
        }

        val loc = portal.spawn()
        val worldName = loc.getWorld().name
        val markerId = idFor(portal) ?: return
        val markerLabel = parent.langMarkerLabel!!.str(portal.name())

        markerSet!!.createMarker(
            markerId,
            markerLabel,
            worldName,
            loc.x,
            loc.y,
            loc.z,
            markerIcon,
            false
        )
    }

    fun removeMarker(portalId: UUID?) {
        removeMarker(idFor(portalId))
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
        for (portal in module!!.allAvailablePortals().filterNotNull()) {
            val p = portal!!
            idSet.add(idFor(p))
            updateMarker(p)
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
