package org.oddlama.vane.bedtime;

import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;

public class BedtimeBlueMapLayer extends ModuleComponent<Bedtime> {

    @ConfigBoolean(def = false, desc = "If the marker set should be hidden by default.")
    public boolean configHideByDefault;

    @LangMessage
    public TranslatedMessage langLayerLabel;

    @LangMessage
    public TranslatedMessage langMarkerLabel;

    private BedtimeBlueMapLayerDelegate delegate = null;

    public BedtimeBlueMapLayer(final Context<Bedtime> context) {
        super(context.group("BlueMap", "Enable BlueMap integration to show player spawnpoints (beds)."));
    }

    public void delayedOnEnable() {
        final var plugin = getModule().getServer().getPluginManager().getPlugin("BlueMap");
        if (plugin == null) {
            return;
        }

        delegate = new BedtimeBlueMapLayerDelegate(this);
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

    public void updateAllMarkers() {
        if (delegate != null) {
            delegate.updateAllMarkers();
        }
    }
}
