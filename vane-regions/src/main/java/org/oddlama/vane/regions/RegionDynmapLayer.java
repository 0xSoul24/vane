package org.oddlama.vane.regions;

import java.util.UUID;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.regions.region.Region;

public class RegionDynmapLayer extends ModuleComponent<Regions> {

    public static final String LAYER_ID = "vane_regions.regions";

    @ConfigInt(def = 35, min = 0, desc = "Layer ordering priority.")
    public int configLayerPriority;

    @ConfigBoolean(def = false, desc = "If the layer should be hidden by default.")
    public boolean configLayerHide;

    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker fill color (0xRRGGBB).")
    public int configFillColor;

    @ConfigDouble(def = 0.05, min = 0.0, max = 1.0, desc = "Area marker fill opacity.")
    public double configFillOpacity;

    @ConfigInt(def = 2, min = 1, desc = "Area marker line weight.")
    public int configLineWeight;

    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker line color (0xRRGGBB).")
    public int configLineColor;

    @ConfigDouble(def = 1.0, min = 0.0, max = 1.0, desc = "Area marker line opacity.")
    public double configLineOpacity;

    @LangMessage
    public TranslatedMessage langLayerLabel;

    @LangMessage
    public TranslatedMessage langMarkerLabel;

    private RegionDynmapLayerDelegate delegate = null;

    public RegionDynmapLayer(final Context<Regions> context) {
        super(
            context.group("Dynmap", "Enable Dynmap integration. Regions will then be shown on a separate Dynmap layer.")
        );
    }

    public void delayedOnEnable() {
        final var plugin = getModule().getServer().getPluginManager().getPlugin("dynmap");
        if (plugin == null) {
            return;
        }

        delegate = new RegionDynmapLayerDelegate(this);
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

    public void updateMarker(final Region region) {
        if (delegate != null) {
            delegate.updateMarker(region);
        }
    }

    public void removeMarker(final UUID regionId) {
        if (delegate != null) {
            delegate.removeMarker(regionId);
        }
    }

    public void removeMarker(final String markerId) {
        if (delegate != null) {
            delegate.removeMarker(markerId);
        }
    }

    public void updateAllMarkers() {
        if (delegate != null) {
            delegate.updateAllMarkers();
        }
    }
}
