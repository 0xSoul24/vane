package org.oddlama.vane.bedtime

import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import java.util.*

class BedtimeBlueMapLayer(context: Context<Bedtime?>) : ModuleComponent<Bedtime?>(
    context.group(
        "BlueMap",
        "Enable BlueMap integration to show player spawnpoints (beds)."
    )
) {
    @ConfigBoolean(def = false, desc = "If the marker set should be hidden by default.")
    var configHideByDefault: Boolean = false

    @LangMessage
    var langLayerLabel: TranslatedMessage? = null

    @LangMessage
    var langMarkerLabel: TranslatedMessage? = null

    private var delegate: BedtimeBlueMapLayerDelegate? = null

    fun delayedOnEnable() {
        val plugin: Plugin = module!!.server.pluginManager.getPlugin("BlueMap") ?: return

        delegate = BedtimeBlueMapLayerDelegate(this)
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

    fun updateMarker(player: OfflinePlayer) {
        if (delegate != null) {
            delegate!!.updateMarker(player)
        }
    }

    fun removeMarker(playerId: UUID?) {
        if (delegate != null && playerId != null) {
            delegate!!.removeMarker(playerId)
        }
    }

    fun updateAllMarkers() {
        if (delegate != null) {
            delegate!!.updateAllMarkers()
        }
    }
}
