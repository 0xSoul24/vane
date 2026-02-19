package org.oddlama.vane.bedtime;

import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;

public class BedtimeDynmapLayer extends ModuleComponent<Bedtime> {

    public static final String LAYER_ID = "vane_bedtime.bedtime";

    @ConfigInt(def = 25, min = 0, desc = "Layer ordering priority.")
    public int configLayerPriority;

    @ConfigBoolean(def = false, desc = "If the layer should be hidden by default.")
    public boolean configLayerHide;

    @ConfigString(def = "house", desc = "The dynmap marker icon.")
    public String configMarkerIcon;

    @LangMessage
    public TranslatedMessage langLayerLabel;

    @LangMessage
    public TranslatedMessage langMarkerLabel;

    private BedtimeDynmapLayerDelegate delegate = null;

    public BedtimeDynmapLayer(final Context<Bedtime> context) {
        super(
            context.group(
                "Dynmap",
                "Enable Dynmap integration. Player spawnpoints (beds) will then be shown on a separate dynmap layer."
            )
        );
    }

    public void delayedOnEnable() {
        final var plugin = getModule().getServer().getPluginManager().getPlugin("dynmap");
        if (plugin == null) {
            return;
        }

        delegate = new BedtimeDynmapLayerDelegate(this);
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

    public void updateMarker(final OfflinePlayer player) {
        if (delegate != null) {
            delegate.updateMarker(player);
        }
    }

    public void removeMarker(final UUID playerId) {
        if (delegate != null) {
            delegate.removeMarker(playerId);
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
