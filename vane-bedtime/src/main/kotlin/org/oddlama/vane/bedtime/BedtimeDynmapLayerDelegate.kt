package org.oddlama.vane.bedtime

import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import org.dynmap.DynmapCommonAPI
import org.dynmap.DynmapCommonAPIListener
import org.dynmap.markers.Marker
import org.dynmap.markers.MarkerAPI
import org.dynmap.markers.MarkerIcon
import org.dynmap.markers.MarkerSet
import java.util.*
import java.util.logging.Level

class BedtimeDynmapLayerDelegate(private val parent: BedtimeDynmapLayer) {
    private var dynmapApi: DynmapCommonAPI? = null
    private var markerApi: MarkerAPI? = null
    private var dynmapEnabled = false
    private var markerSet: MarkerSet? = null
    private var markerIcon: MarkerIcon? = null

    val module: Bedtime?
        get() = parent.module

    fun onEnable(plugin: Plugin?) {
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
            module?.log?.log(Level.WARNING, "Error while enabling dynmap integration!", e)
            return
        }

        if (markerApi == null) {
            return
        }

        module?.log?.info("Enabling dynmap integration")
        dynmapEnabled = true
        createOrLoadLayer()
    }

    fun onDisable() {
        if (!dynmapEnabled) {
            return
        }

        module?.log?.info("Disabling dynmap integration")
        dynmapEnabled = false
        dynmapApi = null
        markerApi = null
    }

    private fun createOrLoadLayer() {
        // Create or retrieve layer
        markerSet = markerApi!!.getMarkerSet(BedtimeDynmapLayer.LAYER_ID)
        if (markerSet == null) {
            markerSet = markerApi!!.createMarkerSet(
                BedtimeDynmapLayer.LAYER_ID,
                parent.langLayerLabel!!.str(),
                null,
                false
            )
        }

        if (markerSet == null) {
            module?.log?.severe("Failed to create dynmap bedtime marker set!")
            return
        }

        // Update attributes
        markerSet!!.markerSetLabel = parent.langLayerLabel!!.str()
        markerSet!!.layerPriority = parent.configLayerPriority
        markerSet!!.hideByDefault = parent.configLayerHide

        // Load marker
        markerIcon = markerApi!!.getMarkerIcon(parent.configMarkerIcon)
        if (markerIcon == null) {
            module?.log?.severe("Failed to load dynmap bedtime marker icon!")
            return
        }

        // Initial update
        updateAllMarkers()
    }

    private fun idFor(playerId: UUID): String {
        return playerId.toString()
    }

    private fun idFor(player: OfflinePlayer): String {
        return idFor(player.uniqueId)
    }

    fun updateMarker(player: OfflinePlayer): Boolean {
        if (!dynmapEnabled) {
            return false
        }

        val loc = player.respawnLocation ?: return false

        val worldName = loc.world.name
        val markerId = idFor(player)
        val markerLabel = parent.langMarkerLabel!!.str(player.name)

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
        return true
    }

    fun removeMarker(playerId: UUID) {
        removeMarker(idFor(playerId))
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
        for (player in this.module!!.offlinePlayersWithValidName) {
            if (updateMarker(player)) {
                idSet.add(idFor(player))
            }
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
