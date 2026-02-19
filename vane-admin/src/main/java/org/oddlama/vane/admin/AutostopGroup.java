package org.oddlama.vane.admin;

import static org.oddlama.vane.util.Conversions.msToTicks;
import static org.oddlama.vane.util.TimeUtil.formatTime;

import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleGroup;

public class AutostopGroup extends ModuleGroup<Admin> {

    @ConfigLong(def = 20 * 60, min = 0, desc = "Delay in seconds after which to stop the server.")
    public long configDelay;

    @LangMessage
    public TranslatedMessage langAborted;

    @LangMessage
    public TranslatedMessage langScheduled;

    @LangMessage
    public TranslatedMessage langStatus;

    @LangMessage
    public TranslatedMessage langStatusNotScheduled;

    @LangMessage
    public TranslatedMessage langShutdown;

    // Variables
    public BukkitTask task = null;
    public long startTime = -1;
    public long stopTime = -1;

    public AutostopGroup(Context<Admin> context) {
        super(context, "Autostop", "Enable automatic server stop after certain time without online players.");
    }

    public long remaining() {
        if (startTime == -1) {
            return -1;
        }

        return stopTime - System.currentTimeMillis();
    }

    public void abort() {
        abort(null);
    }

    public void abort(CommandSender sender) {
        if (task == null) {
            langStatusNotScheduled.send(sender);
            return;
        }

        task.cancel();
        task = null;
        startTime = -1;
        stopTime = -1;

        langAborted.sendAndLog(sender);
    }

    public void schedule() {
        schedule(null);
    }

    public void schedule(CommandSender sender) {
        schedule(sender, configDelay * 1000);
    }

    public void schedule(CommandSender sender, long delay) {
        if (task != null) {
            abort(sender);
        }

        startTime = System.currentTimeMillis();
        stopTime = startTime + delay;
        task = scheduleTask(
            () -> {
                langShutdown.sendAndLog(null);
                getModule().getServer().shutdown();
            },
            msToTicks(delay)
        );

        langScheduled.sendAndLog(sender, "§b" + formatTime(delay));
    }

    public void status(CommandSender sender) {
        if (task == null) {
            langStatusNotScheduled.send(sender);
            return;
        }

        langStatus.send(sender, "§b" + formatTime(remaining()));
    }

    @Override
    public void onModuleEnable() {
        if (getModule().getServer().getOnlinePlayers().isEmpty()) {
            schedule();
        }
    }
}
