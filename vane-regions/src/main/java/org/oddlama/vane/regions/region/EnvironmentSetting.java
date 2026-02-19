package org.oddlama.vane.regions.region;

import org.oddlama.vane.regions.Regions;

public enum EnvironmentSetting {
    // Spawning
    ANIMALS(true),
    MONSTERS(false),

    // Hazards
    EXPLOSIONS(false),
    FIRE(false),
    PVP(true),

    // Environment
    TRAMPLE(false),
    VINE_GROWTH(false);

    private boolean def;

    private EnvironmentSetting(final boolean def) {
        this.def = def;
    }

    public boolean defaultValue() {
        return def;
    }

    public boolean hasOverride() {
        return getOverride() != 0;
    }

    public int getOverride() {
        return Regions.environmentOverrides.getOverride(this);
    }
}
