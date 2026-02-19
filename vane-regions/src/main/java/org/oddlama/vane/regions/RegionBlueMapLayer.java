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

public class RegionBlueMapLayer extends ModuleComponent<Regions> {

    @ConfigBoolean(def = false, desc = "If the marker set should be hidden by default.")
    public boolean configHideByDefault;

    @ConfigBoolean(
        def = true,
        desc = "Set to false to make the area markers visible through terrain and other objects."
    )
    public boolean configDepthTest;

    @ConfigInt(def = 2, min = 1, desc = "Area marker line width.")
    public int configLineWidth;

    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker fill color (0xRRGGBB).")
    public int configFillColor;

    @ConfigDouble(def = 0.1, min = 0.0, max = 1.0, desc = "Area marker fill opacity.")
    public double configFillOpacity;

    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker line color (0xRRGGBB).")
    public int configLineColor;

    @ConfigDouble(def = 1.0, min = 0.0, max = 1.0, desc = "Area marker line opacity.")
    public double configLineOpacity;

    @LangMessage
    public TranslatedMessage langLayerLabel;

    @LangMessage
    public TranslatedMessage langMarkerLabel;

    private RegionBlueMapLayerDelegate delegate = null;

    public RegionBlueMapLayer(final Context<Regions> context) {
        super(context.group("BlueMap", "Enable BlueMap integration."));
    }

    public void delayedOnEnable() {
        final var plugin = getModule().getServer().getPluginManager().getPlugin("BlueMap");
        if (plugin == null) {
            return;
        }

        delegate = new RegionBlueMapLayerDelegate(this);
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

    public void updateAllMarkers() {
        if (delegate != null) {
            delegate.updateAllMarkers();
        }
    }
}
