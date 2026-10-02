package org.oddlama.vane.busybar.hooks

import org.bukkit.plugin.Plugin
import org.oddlama.vane.bedtime.Bedtime

/**
 * Reads vane-bedtime's sleep threshold, so the Bar shows the same requirement players see in chat.
 *
 * @param bedtime the vane-bedtime module.
 */
class BedtimeHook private constructor(private val bedtime: Bedtime) {
    /** Fraction of non-spectators that must sleep to skip the night. */
    fun threshold(): Double = bedtime.configSleepThreshold

    companion object {
        /** Creates the hook when [plugin] is vane-bedtime. */
        fun create(plugin: Plugin): BedtimeHook? = (plugin as? Bedtime)?.let(::BedtimeHook)
    }
}
