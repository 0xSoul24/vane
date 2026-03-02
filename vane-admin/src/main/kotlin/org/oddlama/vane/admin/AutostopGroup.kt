package org.oddlama.vane.admin

import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitTask
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleGroup
import org.oddlama.vane.util.Conversions
import org.oddlama.vane.util.TimeUtil

class AutostopGroup(context: Context<Admin?>) : ModuleGroup<Admin?>(
    context,
    "Autostop",
    "Enable automatic server stop after certain time without online players."
) {
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

    // Variables
    var task: BukkitTask? = null
    var startTime: Long = -1
    var stopTime: Long = -1

    fun remaining(): Long {
        if (startTime == -1L) {
            return -1
        }

        return stopTime - System.currentTimeMillis()
    }

    @JvmOverloads
    fun abort(sender: CommandSender? = null) {
        if (task == null) {
            langStatusNotScheduled!!.send(sender)
            return
        }

        task!!.cancel()
        task = null
        startTime = -1
        stopTime = -1

        langAborted!!.sendAndLog(sender)
    }

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
                module!!.server.shutdown()
            },
            Conversions.msToTicks(delay)
        )

        langScheduled!!.sendAndLog(sender, "§b" + TimeUtil.formatTime(delay))
    }

    fun status(sender: CommandSender?) {
        if (task == null) {
            langStatusNotScheduled!!.send(sender)
            return
        }

        langStatus!!.send(sender, "§b" + TimeUtil.formatTime(remaining()))
    }

    override fun onModuleEnable() {
        if (module!!.server.onlinePlayers.isEmpty()) {
            schedule()
        }
    }
}
