package org.oddlama.vane.regions

import org.bukkit.plugin.Plugin
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.region.Region
import java.util.*

class RegionDynmapLayer(context: Context<Regions?>) : ModuleComponent<Regions?>(
    context.group("Dynmap", "Enable Dynmap integration. Regions will then be shown on a separate Dynmap layer.")
) {
    @ConfigInt(def = 35, min = 0, desc = "Layer ordering priority.")
    var configLayerPriority: Int = 0

    @ConfigBoolean(def = false, desc = "If the layer should be hidden by default.")
    var configLayerHide: Boolean = false

    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker fill color (0xRRGGBB).")
    var configFillColor: Int = 0

    @ConfigDouble(def = 0.05, min = 0.0, max = 1.0, desc = "Area marker fill opacity.")
    var configFillOpacity: Double = 0.0

    @ConfigInt(def = 2, min = 1, desc = "Area marker line weight.")
    var configLineWeight: Int = 0

    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker line color (0xRRGGBB).")
    var configLineColor: Int = 0

    @ConfigDouble(def = 1.0, min = 0.0, max = 1.0, desc = "Area marker line opacity.")
    var configLineOpacity: Double = 0.0

    @LangMessage
    var langLayerLabel: TranslatedMessage? = null

    @LangMessage
    var langMarkerLabel: TranslatedMessage? = null

    private var delegate: RegionDynmapLayerDelegate? = null

    fun delayedOnEnable() {
        val plugin: Plugin = module!!.server.pluginManager.getPlugin("dynmap") ?: return

        delegate = RegionDynmapLayerDelegate(this)
        delegate!!.onEnable(plugin)
    }

    public override fun onEnable() {
        scheduleNextTick { this.delayedOnEnable() }
    }

    public override fun onDisable() {
        if (delegate != null) {
            delegate!!.onDisable()
            delegate = null
        }
    }

    fun updateMarker(region: Region) {
        if (delegate != null) {
            delegate!!.updateMarker(region)
        }
    }

    fun removeMarker(regionId: UUID) {
        if (delegate != null) {
            delegate!!.removeMarker(regionId)
        }
    }

    fun removeMarker(markerId: String?) {
        if (delegate != null) {
            delegate!!.removeMarker(markerId)
        }
    }

    fun updateAllMarkers() {
        if (delegate != null) {
            delegate!!.updateAllMarkers()
        }
    }

    companion object {
        const val LAYER_ID: String = "vane_regions.regions"
    }
}
