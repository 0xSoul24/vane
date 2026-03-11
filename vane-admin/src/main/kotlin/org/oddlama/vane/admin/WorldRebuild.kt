package org.oddlama.vane.admin

import org.bukkit.Material
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.msToTicks
import org.oddlama.vane.util.Nms
import kotlin.Comparator
import kotlin.Int
import kotlin.Long
import kotlin.comparisons.compareValues
import kotlin.math.exp
import kotlin.math.max

class WorldRebuild(context: Context<Admin?>) : Listener<Admin?>(
    context.group(
        "WorldRebuild",
        "Instead of cancelling explosions, the world will regenerate after a short amount of time."
    )
) {
    @ConfigLong(def = 2000, min = 0, desc = "Delay in milliseconds until the world will be rebuilt.")
    private val configDelay: Long = 0

    @ConfigDouble(
        def = 0.175,
        min = 0.0,
        desc = "Determines rebuild speed. Higher falloff means faster transition to quicker rebuild. After n blocks, the delay until the next block will be d_n = delay * exp(-x * delay_falloff). For example 0.0 will result in same delay for every block."
    )
    private val configDelayFalloff = 0.0

    @ConfigLong(
        def = 50,
        min = 50,
        desc = "Minimum delay in milliseconds between rebuilding two blocks. Anything <= 50 milliseconds will be one tick."
    )
    private val configMinDelay: Long = 0

    private val rebuilders: MutableList<Rebuilder> = ArrayList<Rebuilder>()

    fun rebuild(blocks: MutableList<Block>) {
        // Store a snapshot of all block states
        val states = ArrayList<BlockState>()
        for (block in blocks) {
            states.add(block.state)
        }

        // Set everything to air without triggering physics
        for (block in blocks) {
            Nms.setAirNoDrops(block)
        }

        // Schedule rebuild
        rebuilders.add(Rebuilder(states))
    }

    public override fun onDisable() {
        // Finish all pending rebuilds now!
        for (r in ArrayList<Rebuilder>(rebuilders)) {
            r.finishNow()
        }
        rebuilders.clear()
    }

    inner class Rebuilder(private var states: MutableList<BlockState>) : Runnable {
        private var task: BukkitTask? = null
        private var amountRebuild: Long = 0

        init {
            // If no states to rebuild, skip initialization.
            if (this.states.isNotEmpty()) {
                // Find top center point for rebuild order reference
                val center = Vector(0, 0, 0)
                var maxY = 0
                for (state in this.states) {
                    maxY = max(maxY, state.y)
                    center.add(state.location.toVector())
                }
                center.multiply(1.0 / this.states.size)
                center.setY(maxY + 1)

                // Sort blocks to rebuild them in an ordered fashion
                this.states.sortWith(RebuildComparator(center))

                // Initialize delay
                task = this@WorldRebuild.module!!.scheduleTask(this, msToTicks(configDelay))
            }
        }

        private fun finish() {
            task = null
            this@WorldRebuild.rebuilders.remove(this)
        }

        private fun rebuildNextBlock() {
            rebuildBlock(states.removeAt(states.size - 1))
        }

        private fun rebuildBlock(state: BlockState) {
            val block = state.block
            ++amountRebuild

            // Break any block that isn't air first
            if (block.type != Material.AIR) {
                block.breakNaturally()
            }

            // Force update without physics to set a block type
            state.update(true, false)
            // Second update forces block state specific update
            state.update(true, false)

            // Play sound
            block
                .world
                .playSound(
                    block.location,
                    block.blockSoundGroup.placeSound,
                    SoundCategory.BLOCKS,
                    1.0f,
                    0.8f
                )
        }

        fun finishNow() {
            if (task != null) {
                task!!.cancel()
            }

            for (state in states) {
                rebuildBlock(state)
            }

            finish()
        }

        override fun run() {
            if (states.isEmpty()) {
                finish()
            } else {
                // Rebuild next block
                rebuildNextBlock()

                // Adjust delay
                val delay = msToTicks(
                    max(configMinDelay, (configDelay * exp(-amountRebuild * configDelayFalloff)).toInt().toLong())
                )
                this@WorldRebuild.module!!.scheduleTask(this, delay)
            }
        }
    }

    class RebuildComparator(private val referencePoint: Vector) : Comparator<BlockState?> {
        override fun compare(a: BlockState?, b: BlockState?): Int {
            // Handle nulls defensively
            if (a == null && b == null) return 0
            if (a == null) return -1
            if (b == null) return 1

            // Sort by distance to top-most center. The Last block will be rebuilt first.
            val da = a.location.toVector().subtract(referencePoint).lengthSquared()
            val db = b.location.toVector().subtract(referencePoint).lengthSquared()
            return compareValues(da, db)
        }
    }
}
