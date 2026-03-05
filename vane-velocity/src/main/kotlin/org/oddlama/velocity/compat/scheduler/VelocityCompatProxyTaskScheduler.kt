package org.oddlama.velocity.compat.scheduler

import com.velocitypowered.api.scheduler.Scheduler
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.scheduler.ProxyScheduledTask
import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler
import java.util.concurrent.TimeUnit

class VelocityCompatProxyTaskScheduler(val scheduler: Scheduler) : ProxyTaskScheduler {
    override fun runAsync(owner: VaneProxyPlugin?, task: Runnable?): ProxyScheduledTask? {
        // If owner or task is null, we cannot schedule, so follow a safe null-return policy.
        if (owner == null || task == null) return null

        // https://velocitypowered.com/wiki/developers/task-scheduling/
        // "On Velocity, there is no main thread. All tasks run using the
        // Velocity Scheduler are thus run asynchronously."
        return schedule(owner, task, 0, TimeUnit.SECONDS)
    }

    override fun schedule(owner: VaneProxyPlugin?, task: Runnable?, delay: Long, unit: TimeUnit?): ProxyScheduledTask? {
        if (owner == null || task == null || unit == null) return null
        val velocityTask = scheduler.buildTask(owner, task).delay(delay, unit).schedule()
        return VelocityCompatScheduledTask(velocityTask)
    }

    override fun schedule(
        owner: VaneProxyPlugin?,
        task: Runnable?,
        delay: Long,
        period: Long,
        unit: TimeUnit?
    ): ProxyScheduledTask? {
        if (owner == null || task == null || unit == null) return null
        val velocityTask = scheduler
            .buildTask(owner, task)
            .delay(delay, unit)
            .repeat(period, unit)
            .schedule()
        return VelocityCompatScheduledTask(velocityTask)
    }
}
