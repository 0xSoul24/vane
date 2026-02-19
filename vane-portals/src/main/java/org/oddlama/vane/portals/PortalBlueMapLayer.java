package org.oddlama.vane.portals;

import java.util.UUID;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.portal.Portal;

public class PortalBlueMapLayer extends ModuleComponent<Portals> {

    @ConfigBoolean(def = false, desc = "If the marker set should be hidden by default.")
    public boolean configHideByDefault;

    @LangMessage
    public TranslatedMessage langLayerLabel;

    @LangMessage
    public TranslatedMessage langMarkerLabel;

    private PortalBlueMapLayerDelegate delegate = null;

    public PortalBlueMapLayer(final Context<Portals> context) {
        super(context.group("BlueMap", "Enable BlueMap integration to show public portals."));
    }

    public void delayedOnEnable() {
        final var plugin = getModule().getServer().getPluginManager().getPlugin("BlueMap");
        if (plugin == null) {
            return;
        }

        delegate = new PortalBlueMapLayerDelegate(this);
        delegate.onEnable(plugin);
    }

    @Override
    public void onEnable() {
        scheduleNextTick(this::delayedOnEnable);
    }

    @Override
    public void onDisable() {
        if (delegate != null) {
            delegate.onDisable();
            delegate = null;
        }
    }

    public void updateMarker(final Portal portal) {
        if (delegate != null) {
            delegate.updateMarker(portal);
        }
    }

    public void removeMarker(final UUID portalId) {
        if (delegate != null) {
            delegate.removeMarker(portalId);
        }
    }

    public void updateAllMarkers() {
        if (delegate != null) {
            delegate.updateAllMarkers();
        }
    }
}
