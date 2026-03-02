package org.oddlama.vane.bedtime

import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import java.util.*

class BedtimeDynmapLayer(context: Context<Bedtime?>) : ModuleComponent<Bedtime?>(
    context.group(
        "Dynmap",
        "Enable Dynmap integration. Player spawnpoints (beds) will then be shown on a separate dynmap layer."
    )
) {
    @ConfigInt(def = 25, min = 0, desc = "Layer ordering priority.")
    var configLayerPriority: Int = 0

    @ConfigBoolean(def = false, desc = "If the layer should be hidden by default.")
    var configLayerHide: Boolean = false

    @ConfigString(def = "house", desc = "The dynmap marker icon.")
    var configMarkerIcon: String? = null

    @LangMessage
    var langLayerLabel: TranslatedMessage? = null

    @LangMessage
    var langMarkerLabel: TranslatedMessage? = null

    private var delegate: BedtimeDynmapLayerDelegate? = null

    fun delayedOnEnable() {
        val plugin: Plugin = module!!.server.pluginManager.getPlugin("dynmap") ?: return

        delegate = BedtimeDynmapLayerDelegate(this)
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

    fun updateMarker(player: OfflinePlayer?) {
        if (delegate != null && player != null) {
            delegate!!.updateMarker(player)
        }
    }

    fun removeMarker(playerId: UUID?) {
        if (delegate != null && playerId != null) {
            delegate!!.removeMarker(playerId)
        }
    }

    fun removeMarker(markerId: String?) {
        if (delegate != null && markerId != null) {
            delegate!!.removeMarker(markerId)
        }
    }

    fun updateAllMarkers() {
        if (delegate != null) {
            delegate!!.updateAllMarkers()
        }
    }

    companion object {
        const val LAYER_ID: String = "vane_bedtime.bedtime"
    }
}
