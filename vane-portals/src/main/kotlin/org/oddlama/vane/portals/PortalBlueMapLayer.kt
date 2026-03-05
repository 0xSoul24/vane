package org.oddlama.vane.portals

import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.portal.Portal
import java.util.*

class PortalBlueMapLayer(context: Context<Portals?>) :
    ModuleComponent<Portals?>(context.group("BlueMap", "Enable BlueMap integration to show public portals.")) {
    @JvmField
    @ConfigBoolean(def = false, desc = "If the marker set should be hidden by default.")
    var configHideByDefault: Boolean = false

    @JvmField
    @LangMessage
    var langLayerLabel: TranslatedMessage? = null

    @JvmField
    @LangMessage
    var langMarkerLabel: TranslatedMessage? = null

    private var delegate: PortalBlueMapLayerDelegate? = null

    fun delayedOnEnable() {
        if (module!!.server.pluginManager.getPlugin("BlueMap") == null) return

        delegate = PortalBlueMapLayerDelegate(this)
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

    fun updateMarker(portal: Portal) {
        if (delegate != null) {
            delegate!!.updateMarker(portal)
        }
    }

    fun removeMarker(portalId: UUID?) {
        if (delegate != null) {
            delegate!!.removeMarker(portalId)
        }
    }

    @Suppress("unused")
    fun updateAllMarkers() {
        if (delegate != null) {
            delegate!!.updateAllMarkers()
        }
    }
}
