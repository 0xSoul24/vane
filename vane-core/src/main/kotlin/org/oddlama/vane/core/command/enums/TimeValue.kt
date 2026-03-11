package org.oddlama.vane.core.command.enums

enum class TimeValue(val ticks: Int) {
    Dawn(23000),
    Day(1000),
    Noon(6000),
    Afternoon(9000),
    Dusk(13000),
    Night(14000),
    Midnight(18000);

    /** The lowercase name used in-game (tab-complete suggestions, argument parsing). */
    val displayName: String get() = name.lowercase()
}
