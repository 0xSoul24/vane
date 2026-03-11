package org.oddlama.vane.proxycore

import org.oddlama.vane.proxycore.scheduler.ProxyScheduledTask
import org.oddlama.vane.util.formatTime
import java.io.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

@Suppress("unused")
class Maintenance(private val plugin: VaneProxyPlugin) {
    private val file = File("./.maintenance")
    private val taskEnable = TaskEnable()
    private val taskNotify = TaskNotify()
    private var enabled = false
    private var start: Long = 0

    private var duration: Long? = 0L

    fun start(): Long {
        return start
    }

    fun duration(): Long? {
        return duration
    }

    fun enabled(): Boolean {
        return enabled
    }

    fun enable() {
        enabled = true

        // Kick all players
        val kickMessage = formatMessage(MESSAGE_KICK)
        plugin.proxy.players?.forEach { player ->
            player?.disconnect(kickMessage)
        }

        plugin.getLogger().log(Level.INFO, "Maintenance enabled!")
    }

    fun disable() {
        start = 0
        duration = 0L
        enabled = false

        taskEnable.cancel()
        taskNotify.cancel()

        // Delete file
        file.delete()
    }

    fun abort() {
        if (start == 0L) {
            return
        }

        if (start - System.currentTimeMillis() > 0) {
            // Broadcast message (only if not started yet)
            plugin.proxy.broadcast(MESSAGE_ABORTED)
        }

        // Disable maintenance (just to be on the safe side)
        disable()

        plugin.getLogger().log(Level.INFO, "Maintenance disabled!")
    }

    fun schedule(startMillis: Long, durationMillis: Long?) {
        if (durationMillis == null && enabled) {
            plugin.getLogger().log(Level.WARNING, "Maintenance already enabled!")
            return
        }

        // Schedule maintenance
        enabled = false
        start = startMillis
        duration = durationMillis

        // Save to file
        save()

        // Start tasks
        taskEnable.schedule()

        if (durationMillis != null) {
            taskNotify.schedule()
        }
    }

    fun load() {
        if (file.exists()) {
            // Recover maintenance times

            try {
                FileReader(file).use { fileReader ->
                    BufferedReader(fileReader).use { reader ->
                        start = reader.readLine().toLong()
                        val durationLine = reader.readLine()
                        if (durationLine != null) {
                            duration = durationLine.toLong()
                        } else {
                            // We have no duration, run until stopped
                            duration = null
                            enable()
                            return
                        }
                    }
                }
            } catch (e: IOException) {
                plugin.getLogger().log(Level.WARNING, "Failed to read maintenance file", e)
                disable()
                return
            } catch (e: NumberFormatException) {
                plugin.getLogger().log(Level.WARNING, "Invalid maintenance file contents", e)
                disable()
                return
            }

            val delta = System.currentTimeMillis() - start
            if (delta < 0) {
                // Maintenance scheduled but not active
                schedule(start, duration)
            } else if (delta - duration!! < 0) {
                // Maintenance still active
                enable()
            } else {
                // Maintenance already over
                disable()
            }
        } else {
            disable()
        }
    }

    fun save() {
        // create and write file
        try {
            FileWriter(file).use { writer ->
                if (duration != null) {
                    writer.write(start.toString() + "\n" + duration)
                } else {
                    writer.write(start.toString())
                }
            }
        } catch (e: IOException) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save maintenance state to file", e)
        }
    }

    fun formatMessage(message: String): String {
        var timespan = start - System.currentTimeMillis()
        val time: String

        if (timespan <= 0) {
            time = "Now"
        } else {
            if (timespan % 1000 >= 500) {
                timespan += 1000
            }
            time = formatTime(timespan)
        }

        val durationString: String?
        val remainingString: String?
        if (duration != null) {
            var remaining = duration!! + (start - System.currentTimeMillis())
            if (remaining > duration!!) {
                remaining = duration!!
            } else if (remaining < 0) {
                remaining = 0
            }

            durationString = formatTime(duration!!)
            remainingString = formatTime(remaining)
        } else {
            durationString = "Indefinite"
            remainingString = "Indefinite"
        }

        return message
            .replace("%MOTD%", MOTD)
            .replace("%time%", time)
            .replace("%duration%", durationString)
            .replace("%remaining%", remainingString)
    }

    inner class TaskNotify : Runnable {
        private var task: ProxyScheduledTask? = null
        private var notifyTime: Long = -1

        @Synchronized
        override fun run() {
            // Broadcast message
            plugin
                .proxy
                .broadcast(formatMessage(if (notifyTime <= SHUTDOWN_THRESHOLD) MESSAGE_SHUTDOWN else MESSAGE_SCHEDULED))

            // Schedule next time
            schedule()
        }

        @Synchronized
        fun cancel() {
            if (task != null) {
                task!!.cancel()
                task = null

                notifyTime = -1
            }
        }

        @Synchronized
        fun schedule() {
            // cancel if running
            cancel()

            // subtract 500 millis, so we will never "forget" one step
            val timespan = start - System.currentTimeMillis() - 500

            if (notifyTime < 0) {
                // First schedule
                plugin.proxy.broadcast(formatMessage(MESSAGE_SCHEDULED))
                notifyTime = timespan
            }

            if ((nextNotifyTime().also { notifyTime = it }) < 0) {
                // No next time
                return
            }

            // Schedule for next time
            val scheduler = plugin.proxy.scheduler
            if (scheduler == null) {
                plugin.getLogger().log(Level.SEVERE, "No scheduler available to schedule maintenance notifications")
                return
            }

            task = scheduler.schedule(plugin, this, timespan - notifyTime, TimeUnit.MILLISECONDS)
        }

        fun nextNotifyTime(): Long {
            if (notifyTime < 0) {
                return -1
            }

            for (t in NOTIFY_TIMES) {
                if (notifyTime - t > 0) {
                    return t
                }
            }

            return -1
        }
    }

    inner class TaskEnable : Runnable {
        private var task: ProxyScheduledTask? = null

        @Synchronized
        override fun run() {
            this@Maintenance.enable()
            task = null
        }

        @Synchronized
        fun cancel() {
            if (task != null) {
                task!!.cancel()
                task = null
            }
        }

        @Synchronized
        fun schedule() {
            // Cancel if running
            cancel()

            // New task
            var timespan = this@Maintenance.start() - System.currentTimeMillis()
            if (timespan < 0) {
                timespan = 0
            }

            val scheduler = plugin.proxy.scheduler
            if (scheduler == null) {
                plugin.getLogger().log(Level.SEVERE, "No scheduler available to schedule maintenance enable task")
                return
            }

            task = scheduler.schedule(plugin, this, timespan, TimeUnit.MILLISECONDS)
        }
    }

    companion object {
        val NOTIFY_TIMES: LongArray = longArrayOf(
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
        )
        var SHUTDOWN_THRESHOLD: Long = 10000L // MESSAGE_SHUTDOWN if <= 10 seconds
        var MESSAGE_ABORTED: String = "§7> §cServer maintenance §l§6CANCELLED§r§c!"

        var MESSAGE_INFO: String = "§7>" +
                "\n§7> §cScheduled maintenance in: §6%time%" +
                "\n§7> §cExpected time remaining: §6%remaining%" +
                "\n§7>"

        var MESSAGE_SCHEDULED: String = "§7>" +
                "\n§7> §e\u21af§r §6§lMaintenance active§r §e\u21af§r" +
                "\n§7>" +
                "\n§7> §cScheduled maintenance in: §6%time%" +
                "\n§7> §cExpected duration: §6%duration%" +
                "\n§7>"

        var MESSAGE_SHUTDOWN: String = "§7> §cShutdown in §6%time%§c!"

        var MESSAGE_KICK: String =
            "§e\u21af§r §6§lMaintenance active§r §e\u21af§r" + "\n§cExpected duration: §6%duration%"

        @JvmField
        var MOTD: String =
            "§e\u21af§r §6§lMaintenance active§r §e\u21af§r" + "\n§cExpected time remaining: §6%remaining%"

        var MESSAGE_CONNECT: String = "%MOTD%" + "\n" + "\n§7Please try again later."
    }
}
