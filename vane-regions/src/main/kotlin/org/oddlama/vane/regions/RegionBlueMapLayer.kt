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

class RegionBlueMapLayer(context: Context<Regions?>) :
    ModuleComponent<Regions?>(context.group("BlueMap", "Enable BlueMap integration.")) {
    @ConfigBoolean(def = false, desc = "If the marker set should be hidden by default.")
    var configHideByDefault: Boolean = false

    @ConfigBoolean(
        def = true,
        desc = "Set to false to make the area markers visible through terrain and other objects."
    )
    var configDepthTest: Boolean = false

    @ConfigInt(def = 2, min = 1, desc = "Area marker line width.")
    var configLineWidth: Int = 0

    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker fill color (0xRRGGBB).")
    var configFillColor: Int = 0

    @ConfigDouble(def = 0.1, min = 0.0, max = 1.0, desc = "Area marker fill opacity.")
    var configFillOpacity: Double = 0.0

    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker line color (0xRRGGBB).")
    var configLineColor: Int = 0

    @ConfigDouble(def = 1.0, min = 0.0, max = 1.0, desc = "Area marker line opacity.")
    var configLineOpacity: Double = 0.0

    @LangMessage
    var langLayerLabel: TranslatedMessage? = null

    @LangMessage
    var langMarkerLabel: TranslatedMessage? = null

    private var delegate: RegionBlueMapLayerDelegate? = null

    fun delayedOnEnable() {
        val plugin: Plugin = module!!.server.pluginManager.getPlugin("BlueMap") ?: return

        delegate = RegionBlueMapLayerDelegate(this)
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

    fun updateAllMarkers() {
        if (delegate != null) {
            delegate!!.updateAllMarkers()
        }
    }
}
