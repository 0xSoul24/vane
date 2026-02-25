package org.oddlama.vane.util

import org.bukkit.World
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.math.cos

object WorldUtil {
    private val runningTimeChangeTasks = HashMap<UUID?, BukkitTask?>()

    @JvmStatic
    fun changeTimeSmoothly(
        world: World,
        plugin: Plugin,
        worldTicks: Long,
        interpolationTicks: Long
    ): Boolean {
        synchronized(runningTimeChangeTasks) {
            if (runningTimeChangeTasks.containsKey(world.uid)) {
                return false
            }
            // Calculate relative time from and to
            var relTo = worldTicks
            val relFrom = world.time
            if (relTo <= relFrom) {
                relTo += 24000
            }

            // Calculate absolute values
            val deltaTicks = relTo - relFrom
            val absoluteFrom = world.fullTime
            val absoluteTo = absoluteFrom - relFrom + relTo

            // Task to advance time every tick
            val task = plugin
                .server
                .scheduler
                .runTaskTimer(
                    plugin,
                    object : Runnable {
                        private var elapsed: Long = 0

                        override fun run() {
                            // Remove a task if we finished interpolation
                            if (elapsed > interpolationTicks) {
                                synchronized(runningTimeChangeTasks) {
                                    runningTimeChangeTasks.remove(world.uid)!!.cancel()
                                }
                            }

                            // Make the transition smooth by applying a cosine
                            val linDelta = elapsed.toFloat() / interpolationTicks
                            val delta = (1f - cos(Math.PI * linDelta).toFloat()) / 2f

                            val curTicks = absoluteFrom + (deltaTicks * delta).toLong()
                            world.fullTime = curTicks
                            ++elapsed
                        }
                    },
                    1,
                    1
                )
            runningTimeChangeTasks.put(world.uid, task)
        }

        return true
    }
}
