package org.oddlama.vane.util;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class WorldUtil {

    private static final HashMap<UUID, BukkitTask> runningTimeChangeTasks = new HashMap<>();

    public static boolean changeTimeSmoothly(
        final World world,
        final Plugin plugin,
        final long worldTicks,
        final long interpolationTicks
    ) {
        synchronized (runningTimeChangeTasks) {
            if (runningTimeChangeTasks.containsKey(world.getUID())) {
                return false;
            }

            // Calculate relative time from and to
            var relTo = worldTicks;
            var relFrom = world.getTime();
            if (relTo <= relFrom) {
                relTo += 24000;
            }

            // Calculate absolute values
            final var deltaTicks = relTo - relFrom;
            final var absoluteFrom = world.getFullTime();
            final var absoluteTo = absoluteFrom - relFrom + relTo;

            // Task to advance time every tick
            BukkitTask task = plugin
                .getServer()
                .getScheduler()
                .runTaskTimer(
                    plugin,
                    new Runnable() {
                        private long elapsed = 0;

                        @Override
                        public void run() {
                            // Remove a task if we finished interpolation
                            if (elapsed > interpolationTicks) {
                                synchronized (runningTimeChangeTasks) {
                                    runningTimeChangeTasks.remove(world.getUID()).cancel();
                                }
                            }

                            // Make the transition smooth by applying a cosine
                            var linDelta = (float) elapsed / interpolationTicks;
                            var delta = (1f - (float) Math.cos(Math.PI * linDelta)) / 2f;

                            var curTicks = absoluteFrom + (long) (deltaTicks * delta);
                            world.setFullTime(curTicks);
                            ++elapsed;
                        }
                    },
                    1,
                    1
                );

            runningTimeChangeTasks.put(world.getUID(), task);
        }

        return true;
    }
}
