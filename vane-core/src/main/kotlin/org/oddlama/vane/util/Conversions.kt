package org.oddlama.vane.util

object Conversions {
    @JvmStatic
    fun msToTicks(ms: Long): Long {
        return ms / 50
    }

    @JvmStatic
    fun ticksToMs(ticks: Long): Long {
        return ticks * 50
    }

    @JvmStatic
    fun expForLevel(level: Int): Int {
        return if (level < 17) {
            level * level + 6 * level
        } else if (level < 32) {
            (2.5 * level * level - 40.5 * level).toInt() + 360
        } else {
            (4.5 * level * level - 162.5 * level).toInt() + 2220
        }
    }
}
