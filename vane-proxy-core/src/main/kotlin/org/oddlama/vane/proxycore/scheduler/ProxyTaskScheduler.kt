package org.oddlama.vane.proxycore.scheduler

import org.oddlama.vane.proxycore.VaneProxyPlugin
import java.util.concurrent.TimeUnit

interface ProxyTaskScheduler {
    fun runAsync(owner: VaneProxyPlugin?, task: Runnable?): ProxyScheduledTask?

    fun schedule(owner: VaneProxyPlugin?, task: Runnable?, delay: Long, unit: TimeUnit?): ProxyScheduledTask?

    fun schedule(
        owner: VaneProxyPlugin?,
        task: Runnable?,
        delay: Long,
        period: Long,
        unit: TimeUnit?
    ): ProxyScheduledTask?
}
