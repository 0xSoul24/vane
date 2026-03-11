package org.oddlama.vane.core.command.enums

enum class WeatherValue(val storm: Boolean, val thunder: Boolean) {
    Clear(false, false),
    Sun(false, false),
    Rain(true, false),
    Thunder(true, true);

    /** The lowercase name used in-game (tab-complete suggestions, argument parsing). */
    val displayName: String get() = name.lowercase()
}
