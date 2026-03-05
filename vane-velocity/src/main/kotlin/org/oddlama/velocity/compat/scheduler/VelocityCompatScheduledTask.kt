package org.oddlama.velocity.compat.scheduler

import com.velocitypowered.api.scheduler.ScheduledTask
import org.oddlama.vane.proxycore.scheduler.ProxyScheduledTask

class VelocityCompatScheduledTask(val task: ScheduledTask) : ProxyScheduledTask {
    override fun cancel() {
        task.cancel()
    }
}
