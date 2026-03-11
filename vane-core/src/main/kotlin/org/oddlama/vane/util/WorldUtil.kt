package org.oddlama.vane.util

import org.bukkit.World
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.math.PI
import kotlin.math.cos

object WorldUtil {
    private val runningTimeChangeTasks = mutableMapOf<UUID, BukkitTask>()

    @JvmStatic
    fun changeTimeSmoothly(
        world: World,
        plugin: Plugin,
        worldTicks: Long,
        interpolationTicks: Long
    ): Boolean {
        synchronized(runningTimeChangeTasks) {
            if (world.uid in runningTimeChangeTasks) return false

            val relFrom = world.time
            val relTo = if (worldTicks > relFrom) worldTicks else worldTicks + 24000
            val deltaTicks = relTo - relFrom
            val absoluteFrom = world.fullTime

            var elapsed = 0L
            val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
                if (elapsed > interpolationTicks) {
                    synchronized(runningTimeChangeTasks) {
                        runningTimeChangeTasks.remove(world.uid)!!.cancel()
                    }
                }
                val linDelta = elapsed.toFloat() / interpolationTicks
                val delta = (1f - cos(PI * linDelta).toFloat()) / 2f
                world.fullTime = absoluteFrom + (deltaTicks * delta).toLong()
                elapsed++
            }, 1L, 1L)

            runningTimeChangeTasks[world.uid] = task
        }
        return true
    }
}
