package org.oddlama.vane.portals;

import java.util.UUID;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.portal.Portal;

public class PortalDynmapLayer extends ModuleComponent<Portals> {

    public static final String LAYER_ID = "vane_portals.portals";

    @ConfigInt(def = 29, min = 0, desc = "Layer ordering priority.")
    public int configLayerPriority;

    @ConfigBoolean(def = false, desc = "If the layer should be hidden by default.")
    public boolean configLayerHide;

    @ConfigString(def = "compass", desc = "The Dynmap marker icon.")
    public String configMarkerIcon;

    @LangMessage
    public TranslatedMessage langLayerLabel;

    @LangMessage
    public TranslatedMessage langMarkerLabel;

    private PortalDynmapLayerDelegate delegate = null;

    public PortalDynmapLayer(final Context<Portals> context) {
        super(
            context.group(
                "Dynmap",
                "Enable Dynmap integration. Public portals will then be shown on a separate Dynmap layer."
            )
        );
    }

    public void delayedOnEnable() {
        final var plugin = getModule().getServer().getPluginManager().getPlugin("Dynmap");
        if (plugin == null) {
            return;
        }

        delegate = new PortalDynmapLayerDelegate(this);
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
