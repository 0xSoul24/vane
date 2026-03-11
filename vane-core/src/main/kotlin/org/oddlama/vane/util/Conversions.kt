package org.oddlama.vane.util
fun msToTicks(ms: Long): Long = ms / 50
fun ticksToMs(ticks: Long): Long = ticks * 50
fun expForLevel(level: Int): Int = when {
    level < 17 -> level * level + 6 * level
    level < 32 -> (2.5 * level * level - 40.5 * level).toInt() + 360
    else       -> (4.5 * level * level - 162.5 * level).toInt() + 2220
}
