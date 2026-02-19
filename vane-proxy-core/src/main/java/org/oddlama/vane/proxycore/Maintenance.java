package org.oddlama.vane.proxycore;

import static org.oddlama.vane.proxycore.util.TimeUtil.formatTime;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.proxycore.scheduler.ProxyScheduledTask;

public class Maintenance {

    public static final long[] NOTIFY_TIMES = {
        240 * 60000L,
        180 * 60000L,
        120 * 60000L,
        60 * 60000L,
        30 * 60000L,
        15 * 60000L,
        10 * 60000L,
        5 * 60000L,
        4 * 60000L,
        3 * 60000L,
        2 * 60000L,
        60000L,
        30000L,
        10000L,
        5000L,
        4000L,
        3000L,
        2000L,
        1000L,
    };
    public static long SHUTDOWN_THRESHOLD = 10000L; // MESSAGE_SHUTDOWN if <= 10 seconds
    public static String MESSAGE_ABORTED = "§7> §cServer maintenance §l§6CANCELLED§r§c!";

    public static String MESSAGE_INFO =
        "§7>" +
        "\n§7> §cScheduled maintenance in: §6%time%" +
        "\n§7> §cExpected time remaining: §6%remaining%" +
        "\n§7>";

    public static String MESSAGE_SCHEDULED =
        "§7>" +
        "\n§7> §e\u21af§r §6§lMaintenance active§r §e\u21af§r" +
        "\n§7>" +
        "\n§7> §cScheduled maintenance in: §6%time%" +
        "\n§7> §cExpected duration: §6%duration%" +
        "\n§7>";

    public static String MESSAGE_SHUTDOWN = "§7> §cShutdown in §6%time%§c!";

    public static String MESSAGE_KICK =
        "§e\u21af§r §6§lMaintenance active§r §e\u21af§r" + "\n§cExpected duration: §6%duration%";

    public static String MOTD =
        "§e\u21af§r §6§lMaintenance active§r §e\u21af§r" + "\n§cExpected time remaining: §6%remaining%";

    public static String MESSAGE_CONNECT = "%MOTD%" + "\n" + "\n§7Please try again later.";

    private final VaneProxyPlugin plugin;
    private final File file = new File("./.maintenance");
    private final TaskEnable taskEnable = new TaskEnable();
    private final TaskNotify taskNotify = new TaskNotify();
    private boolean enabled = false;
    private long start = 0;

    @Nullable
    private Long duration = 0L;

    public Maintenance(final VaneProxyPlugin plugin) {
        this.plugin = plugin;
    }

    public long start() {
        return start;
    }

    public @Nullable Long duration() {
        return duration;
    }

    public boolean enabled() {
        return enabled;
    }

    public void enable() {
        enabled = true;

        // Kick all players
        final var kickMessage = formatMessage(MESSAGE_KICK);
        for (final var player : plugin.getProxy().getPlayers()) {
            player.disconnect(kickMessage);
        }

        plugin.getLogger().log(Level.INFO, "Maintenance enabled!");
    }

    public void disable() {
        start = 0;
        duration = 0L;
        enabled = false;

        taskEnable.cancel();
        taskNotify.cancel();

        // Delete file
        file.delete();
    }

    public void abort() {
        if (start == 0) {
            return;
        }

        if (start - System.currentTimeMillis() > 0) {
            // Broadcast message (only if not started yet)
            plugin.getProxy().broadcast(MESSAGE_ABORTED);
        }

        // Disable maintenance (just to be on the safe side)
        disable();

        plugin.getLogger().log(Level.INFO, "Maintenance disabled!");
    }

    public void schedule(long startMillis, @Nullable Long durationMillis) {
        if (durationMillis == null && enabled) {
            plugin.getLogger().log(Level.WARNING, "Maintenance already enabled!");
            return;
        }

        // Schedule maintenance
        enabled = false;
        start = startMillis;
        duration = durationMillis;

        // Save to file
        save();

        // Start tasks
        taskEnable.schedule();

        if (durationMillis != null) {
            taskNotify.schedule();
        }
    }

    public void load() {
        if (file.exists()) {
            // Recover maintenance times

            try (final var fileReader = new FileReader(file); final var reader = new BufferedReader(fileReader)) {
                start = Long.parseLong(reader.readLine());
                String durationLine = reader.readLine();
                if (durationLine != null) {
                    duration = Long.parseLong(durationLine);
                } else {
                    // We have no duration, run until stopped
                    duration = null;
                    enable();
                    return;
                }
            } catch (IOException | NumberFormatException e) {
                disable();
                return;
            }

            final var delta = System.currentTimeMillis() - start;
            if (delta < 0) {
                // Maintenance scheduled but not active
                schedule(start, duration);
            } else if (delta - duration < 0) {
                // Maintenance still active
                enable();
            } else {
                // Maintenance already over
                disable();
            }
        } else {
            disable();
        }
    }

    public void save() {
        // create and write file
        try (final FileWriter writer = new FileWriter(file)) {
            if (duration != null) {
                writer.write(start + "\n" + duration);
            } else {
                writer.write(Long.toString(start));
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save maintenance state to file", e);
        }
    }

    public String formatMessage(final String message) {
        var timespan = start - System.currentTimeMillis();
        final String time;

        if (timespan <= 0) {
            time = "Now";
        } else {
            if (timespan % 1000 >= 500) {
                timespan += 1000;
            }
            time = formatTime(timespan);
        }

        String durationString;
        String remainingString;
        if (duration != null) {
            var remaining = duration + (start - System.currentTimeMillis());
            if (remaining > duration) {
                remaining = duration;
            } else if (remaining < 0) {
                remaining = 0;
            }

            durationString = formatTime(duration);
            remainingString = formatTime(remaining);
        } else {
            durationString = "Indefinite";
            remainingString = "Indefinite";
        }

        return message
            .replace("%MOTD%", MOTD)
            .replace("%time%", time)
            .replace("%duration%", durationString)
            .replace("%remaining%", remainingString);
    }

    public class TaskNotify implements Runnable {

        private ProxyScheduledTask task = null;
        private long notifyTime = -1;

        @Override
        public synchronized void run() {
            // Broadcast message
            plugin
                .getProxy()
                .broadcast(formatMessage(notifyTime <= SHUTDOWN_THRESHOLD ? MESSAGE_SHUTDOWN : MESSAGE_SCHEDULED));

            // Schedule next time
            schedule();
        }

        public synchronized void cancel() {
            if (task != null) {
                task.cancel();
                task = null;

                notifyTime = -1;
            }
        }

        public synchronized void schedule() {
            // cancel if running
            cancel();

            // subtract 500 millis, so we will never "forget" one step
            final var timespan = start - System.currentTimeMillis() - 500;

            if (notifyTime < 0) {
                // First schedule
                plugin.getProxy().broadcast(formatMessage(MESSAGE_SCHEDULED));
                notifyTime = timespan;
            }

            if ((notifyTime = nextNotifyTime()) < 0) {
                // No next time
                return;
            }

            // Schedule for next time
            task = plugin
                .getProxy()
                .getScheduler()
                .schedule(plugin, this, timespan - notifyTime, TimeUnit.MILLISECONDS);
        }

        public long nextNotifyTime() {
            if (notifyTime < 0) {
                return -1;
            }

            for (final var t : NOTIFY_TIMES) {
                if (notifyTime - t > 0) {
                    return t;
                }
            }

            return -1;
        }
    }

    public class TaskEnable implements Runnable {

        private ProxyScheduledTask task = null;

        @Override
        public synchronized void run() {
            Maintenance.this.enable();
            task = null;
        }

        synchronized void cancel() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }

        synchronized void schedule() {
            // Cancel if running
            cancel();

            // New task
            var timespan = Maintenance.this.start() - System.currentTimeMillis();
            if (timespan < 0) {
                timespan = 0;
            }

            task = plugin.getProxy().getScheduler().schedule(plugin, this, timespan, TimeUnit.MILLISECONDS);
        }
    }
}
