package org.oddlama.vane.regions.region

import org.oddlama.vane.regions.Regions

enum class EnvironmentSetting(private val def: Boolean) {
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

    fun defaultValue(): Boolean {
        return def
    }

    fun hasOverride(): Boolean {
        return this.override != 0
    }

    val override: Int
        get() = Regions.environmentOverrides?.getOverride(this) ?: 0
}
