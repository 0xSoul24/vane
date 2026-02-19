package org.oddlama.vane.bedtime;

import static org.oddlama.vane.util.WorldUtil.changeTimeSmoothly;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.Nms;

@VaneModule(name = "bedtime", bstats = 8639, configVersion = 3, langVersion = 5, storageVersion = 1)
public class Bedtime extends Module<Bedtime> {

    // One set of sleeping players per world, to keep track
    private HashMap<UUID, HashSet<UUID>> worldSleepers = new HashMap<>();

    // Configuration
    @ConfigDouble(
        def = 0.5,
        min = 0.0,
        max = 1.0,
        desc = "The percentage of sleeping players required to advance time."
    )
    double configSleepThreshold;

    @ConfigLong(
        def = 1000,
        min = 0,
        max = 12000,
        desc = "The target time in ticks to advance to. 1000 is just after sunrise."
    )
    long configTargetTime;

    @ConfigLong(def = 100, min = 0, max = 1200, desc = "The interpolation time in ticks for a smooth change of time.")
    long configInterpolationTicks;

    // Language
    @LangMessage
    private TranslatedMessage langPlayerBedEnter;

    @LangMessage
    private TranslatedMessage langPlayerBedLeave;

    public BedtimeDynmapLayer dynmapLayer;
    public BedtimeBlueMapLayer blueMapLayer;

    public Bedtime() {
        dynmapLayer = new BedtimeDynmapLayer(this);
        blueMapLayer = new BedtimeBlueMapLayer(this);
    }

    public void startCheckWorldTask(final World world) {
        if (enoughPlayersSleeping(world)) {
            scheduleTask(
                () -> {
                    checkWorldNow(world);
                    // Subtract two ticks so this runs one tick before minecraft would
                    // advance time (if all players are asleep), which would effectively cancel
                    // the task.
                },
                100 - 2
            );
        }
    }

    public void checkWorldNow(final World world) {
        // Abort task if condition changed
        if (!enoughPlayersSleeping(world)) {
            return;
        }

        // Let the sun rise, and set weather
        changeTimeSmoothly(world, this, configTargetTime, configInterpolationTicks);
        world.setStorm(false);
        world.setThundering(false);

        // Clear sleepers
        resetSleepers(world);

        // Wakeup players as if they were actually sleeping through the night
        world
            .getPlayers()
            .stream()
            .filter(Player::isSleeping)
            .forEach(p -> {
                // skipSleepTimer = false (-> set sleepCounter to 100)
                // updateSleepingPlayers = false
                Nms.getPlayer(p).stopSleepInBed(false, false);
            });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        final var player = event.getPlayer();
        final var world = player.getWorld();

        // Update marker
        dynmapLayer.updateMarker(player);
        blueMapLayer.updateMarker(player);

        scheduleNextTick(() -> {
            // Register the new player as sleeping
            addSleeping(world, player);
            // Start a sleep check task
            startCheckWorldTask(world);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        removeSleeping(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Start a sleep check task
        startCheckWorldTask(event.getPlayer().getWorld());
    }

    private static String percentageStr(double percentage) {
        return String.format("§6%.2f", 100.0 * percentage) + "%";
    }

    private long getAmountSleeping(final World world) {
        // return world.getPlayers().stream()
        //	.filter(p -> p.getGameMode() != GameMode.SPECTATOR)
        //	.filter(p -> p.isSleeping())
        //	.count();

        final var worldId = world.getUID();
        var sleepers = worldSleepers.get(worldId);
        if (sleepers == null) {
            return 0;
        }
        return sleepers.size();
    }

    private long getPotentialSleepersInWorld(final World world) {
        return world.getPlayers().stream().filter(p -> p.getGameMode() != GameMode.SPECTATOR).count();
    }

    private double getPercentageSleeping(final World world) {
        final var countSleeping = getAmountSleeping(world);
        if (countSleeping == 0) {
            return 0.0;
        }

        return (double) countSleeping / getPotentialSleepersInWorld(world);
    }

    private boolean enoughPlayersSleeping(final World world) {
        return getPercentageSleeping(world) >= configSleepThreshold;
    }

    private void addSleeping(final World world, final Player player) {
        // Add player to sleepers
        final var worldId = world.getUID();
        var sleepers = worldSleepers.computeIfAbsent(worldId, k -> new HashSet<>());

        sleepers.add(player.getUniqueId());

        // Broadcast a sleeping message
        var percent = getPercentageSleeping(world);
        var amountSleeping = getAmountSleeping(world);
        var countRequired = (int) Math.ceil(getPotentialSleepersInWorld(world) * configSleepThreshold);
        langPlayerBedEnter.broadcastWorldActionBar(
            world,
            "§6" + player.getName(),
            "§6" + percentageStr(percent),
            String.valueOf(amountSleeping),
            String.valueOf(countRequired),
            "§6" + world.getName()
        );
    }

    private void removeSleeping(Player player) {
        final var world = player.getWorld();
        final var worldId = world.getUID();

        // Remove player from sleepers
        final var sleepers = worldSleepers.get(worldId);
        if (sleepers == null) {
            // No sleepers in this world. Abort.
            return;
        }

        if (sleepers.remove(player.getUniqueId())) {
            // Broadcast a sleeping message
            var percent = getPercentageSleeping(world);
            var countSleeping = getAmountSleeping(world);
            var countRequired = (int) Math.ceil(getPotentialSleepersInWorld(world) * configSleepThreshold);
            langPlayerBedLeave.broadcastWorldActionBar(
                world,
                "§6" + player.getName(),
                "§6" + percentageStr(percent),
                String.valueOf(countSleeping),
                String.valueOf(countRequired),
                "§6" + world.getName()
            );
        }
    }

    private void resetSleepers(World world) {
        final var worldId = world.getUID();
        final var sleepers = worldSleepers.get(worldId);
        if (sleepers == null) {
            return;
        }

        sleepers.clear();
    }
}
