package org.oddlama.vane.portals

import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.portal.Portal
import java.util.*

class PortalDynmapLayer(context: Context<Portals?>) : ModuleComponent<Portals?>(
    context.group(
        "Dynmap",
        "Enable Dynmap integration. Public portals will then be shown on a separate Dynmap layer."
    )
) {
    @JvmField
    @ConfigInt(def = 29, min = 0, desc = "Layer ordering priority.")
    var configLayerPriority: Int = 0

    @JvmField
    @ConfigBoolean(def = false, desc = "If the layer should be hidden by default.")
    var configLayerHide: Boolean = false

    @JvmField
    @ConfigString(def = "compass", desc = "The Dynmap marker icon.")
    var configMarkerIcon: String? = null

    @JvmField
    @LangMessage
    var langLayerLabel: TranslatedMessage? = null

    @JvmField
    @LangMessage
    var langMarkerLabel: TranslatedMessage? = null

    private var delegate: PortalDynmapLayerDelegate? = null

    fun delayedOnEnable() {
        if (module!!.server.pluginManager.getPlugin("Dynmap") == null) return

        delegate = PortalDynmapLayerDelegate(this)
        delegate!!.onEnable()
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

    fun updateMarker(portal: Portal?) {
        if (delegate != null && portal != null) {
            delegate!!.updateMarker(portal)
        }
    }

    fun removeMarker(portalId: UUID?) {
        if (delegate != null && portalId != null) {
            delegate!!.removeMarker(portalId)
        }
    }

    fun updateAllMarkers() {
        if (delegate != null) {
            delegate!!.updateAllMarkers()
        }
    }

    companion object {
        const val LAYER_ID: String = "vane_portals.portals"
    }
}
