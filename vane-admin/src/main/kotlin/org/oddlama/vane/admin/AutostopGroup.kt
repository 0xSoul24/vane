package org.oddlama.vane.admin

import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitTask
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleGroup
import org.oddlama.vane.util.formatTime
import org.oddlama.vane.util.msToTicks

/**
 * Handles automatic server shutdown scheduling when no players are online.
 */
class AutostopGroup(context: Context<Admin?>) : ModuleGroup<Admin?>(
    context,
    "Autostop",
    "Enable automatic server stop after certain time without online players."
) {
    private val admin: Admin
        get() = requireNotNull(module)

    @ConfigLong(def = 20 * 60, min = 0, desc = "Delay in seconds after which to stop the server.")
    var configDelay: Long = 0

    @LangMessage
    var langAborted: TranslatedMessage? = null

    @LangMessage
    var langScheduled: TranslatedMessage? = null

    @LangMessage
    var langStatus: TranslatedMessage? = null

    @LangMessage
    var langStatusNotScheduled: TranslatedMessage? = null

    @LangMessage
    var langShutdown: TranslatedMessage? = null

    /** Currently scheduled shutdown task, if any. */
    var task: BukkitTask? = null

    /** Timestamp when the current schedule started, or -1 when inactive. */
    var startTime: Long = -1

    /** Timestamp when shutdown is scheduled, or -1 when inactive. */
    var stopTime: Long = -1

    /** Returns remaining milliseconds until shutdown, or -1 when not scheduled. */
    fun remaining(): Long = if (startTime == -1L) -1 else stopTime - System.currentTimeMillis()

    /** Aborts a scheduled shutdown and notifies the sender. */
    @JvmOverloads
    fun abort(sender: CommandSender? = null) {
        val scheduledTask = task
        if (scheduledTask == null) {
            langStatusNotScheduled!!.send(sender)
            return
        }

        scheduledTask.cancel()
        task = null
        startTime = -1
        stopTime = -1

        langAborted!!.sendAndLog(sender)
    }

    /** Schedules a shutdown after the given delay in milliseconds. */
    @JvmOverloads
    fun schedule(sender: CommandSender? = null, delay: Long = configDelay * 1000) {
        if (task != null) {
            abort(sender)
        }

        startTime = System.currentTimeMillis()
        stopTime = startTime + delay
        task = scheduleTask(
            {
                langShutdown!!.sendAndLog(null)
                admin.server.shutdown()
            },
            msToTicks(delay)
        )

        langScheduled!!.sendAndLog(sender, "§b${formatTime(delay)}")
    }

    /** Sends current autostop scheduling status. */
    fun status(sender: CommandSender?) {
        if (task == null) {
            langStatusNotScheduled!!.send(sender)
            return
        }

        langStatus!!.send(sender, "§b${formatTime(remaining())}")
    }

    /** Schedules autostop immediately when no players are online at enable time. */
    override fun onModuleEnable() {
        if (admin.server.onlinePlayers.isEmpty()) {
            schedule()
        }
    }
}
