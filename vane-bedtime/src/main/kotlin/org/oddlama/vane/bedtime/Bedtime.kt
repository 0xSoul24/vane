package org.oddlama.vane.bedtime

import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.util.Nms
import org.oddlama.vane.util.WorldUtil
import java.util.*
import kotlin.math.ceil

@VaneModule(name = "bedtime", bstats = 8639, configVersion = 3, langVersion = 5, storageVersion = 1)
class Bedtime : Module<Bedtime?>() {
    // One set of sleeping players per world, to keep track
    private val worldSleepers = HashMap<UUID?, HashSet<UUID?>>()

    // Configuration
    @ConfigDouble(
        def = 0.5,
        min = 0.0,
        max = 1.0,
        desc = "The percentage of sleeping players required to advance time."
    )
    var configSleepThreshold: Double = 0.0

    @ConfigLong(
        def = 1000,
        min = 0,
        max = 12000,
        desc = "The target time in ticks to advance to. 1000 is just after sunrise."
    )
    var configTargetTime: Long = 0

    @ConfigLong(def = 100, min = 0, max = 1200, desc = "The interpolation time in ticks for a smooth change of time.")
    var configInterpolationTicks: Long = 0

    // Language
    @LangMessage
    private val langPlayerBedEnter: TranslatedMessage? = null

    @LangMessage
    private val langPlayerBedLeave: TranslatedMessage? = null

    var dynmapLayer: BedtimeDynmapLayer = BedtimeDynmapLayer(this)
    var blueMapLayer: BedtimeBlueMapLayer = BedtimeBlueMapLayer(this)

    fun startCheckWorldTask(world: World) {
        if (enoughPlayersSleeping(world)) {
            scheduleTask(
                {
                    checkWorldNow(world)
                },
                (100 - 2).toLong()
            )
        }
    }

    fun checkWorldNow(world: World) {
        // Abort task if condition changed
        if (!enoughPlayersSleeping(world)) {
            return
        }

        // Let the sun rise, and set weather
        WorldUtil.changeTimeSmoothly(world, this, configTargetTime, configInterpolationTicks)
        world.setStorm(false)
        world.isThundering = false

        // Clear sleepers
        resetSleepers(world)

        // Wakeup players as if they were actually sleeping through the night
        world
            .players
            .stream()
            .filter { obj: Player? -> obj!!.isSleeping }
            .forEach { p: Player? ->
                // skipSleepTimer = false (-> set sleepCounter to 100)
                // updateSleepingPlayers = false
                Nms.getPlayer(p!!)!!.stopSleepInBed(false, false)
            }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerBedEnter(event: PlayerBedEnterEvent) {
        val player = event.getPlayer()
        val world = player.world

        // Update marker
        dynmapLayer.updateMarker(player)
        blueMapLayer.updateMarker(player)

        scheduleNextTick {
            // Register the new player as sleeping
            addSleeping(world, player)
            // Start a sleep check task
            startCheckWorldTask(world)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerBedLeave(event: PlayerBedLeaveEvent) {
        removeSleeping(event.getPlayer())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // Start a sleep check task
        startCheckWorldTask(event.getPlayer().world)
    }

    private fun getAmountSleeping(world: World): Long {
        // return world.getPlayers().stream()
        //	.filter(p -> p.getGameMode() != GameMode.SPECTATOR)
        //	.filter(p -> p.isSleeping())
        //	.count();

        val worldId = world.uid
        val sleepers = worldSleepers[worldId] ?: return 0
        return sleepers.size.toLong()
    }

    private fun getPotentialSleepersInWorld(world: World): Long {
        return world.players.stream().filter { p: Player? -> p!!.gameMode != GameMode.SPECTATOR }.count()
    }

    private fun getPercentageSleeping(world: World): Double {
        val countSleeping = getAmountSleeping(world)
        if (countSleeping == 0L) {
            return 0.0
        }

        return countSleeping.toDouble() / getPotentialSleepersInWorld(world)
    }

    private fun enoughPlayersSleeping(world: World): Boolean {
        return getPercentageSleeping(world) >= configSleepThreshold
    }

    private fun addSleeping(world: World, player: Player) {
        // Add player to sleepers
        val worldId = world.uid
        val sleepers = worldSleepers.computeIfAbsent(worldId) { _: UUID? -> HashSet<UUID?>() }

        sleepers.add(player.uniqueId)

        // Broadcast a sleeping message
        val percent = getPercentageSleeping(world)
        val amountSleeping = getAmountSleeping(world)
        val countRequired = ceil(getPotentialSleepersInWorld(world) * configSleepThreshold).toInt()
        broadcastSleepingMessage(langPlayerBedEnter, world, player, percent, amountSleeping, countRequired)
    }

    private fun removeSleeping(player: Player) {
        val world = player.world
        val worldId = world.uid

        // Remove player from sleepers
        val sleepers = worldSleepers[worldId] ?: // No sleepers in this world. Abort.
        return

        if (sleepers.remove(player.uniqueId)) {
            // Broadcast a sleeping message
            val percent = getPercentageSleeping(world)
            val countSleeping = getAmountSleeping(world)
            val countRequired = ceil(getPotentialSleepersInWorld(world) * configSleepThreshold).toInt()
            broadcastSleepingMessage(langPlayerBedLeave, world, player, percent, countSleeping, countRequired)
        }
    }

    private fun resetSleepers(world: World) {
        val worldId = world.uid
        val sleepers = worldSleepers[worldId] ?: return

        sleepers.clear()
    }

    companion object {
        private fun percentageStr(percentage: Double): String {
            return String.format("§6%.2f", 100.0 * percentage) + "%"
        }
    }

    // New function to eliminate duplication in the construction/sending of the action message
    private fun broadcastSleepingMessage(
        message: TranslatedMessage?,
        world: World,
        player: Player,
        percent: Double,
        amountSleeping: Long,
        countRequired: Int
    ) {
        message!!.broadcastWorldActionBar(
            world,
            "§6" + player.name,
            "§6" + percentageStr(percent),
            amountSleeping.toString(),
            countRequired.toString(),
            "§6" + world.name
        )
    }
}
