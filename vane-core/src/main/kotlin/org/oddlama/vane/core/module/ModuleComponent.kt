package org.oddlama.vane.core.module

import org.bukkit.NamespacedKey
import org.bukkit.scheduler.BukkitTask
import org.json.JSONObject
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import java.io.IOException
import java.util.function.Consumer

abstract class ModuleComponent<T : Module<T?>?>(context: Context<T?>?) {
    private var context: Context<T?>? = null

    init {
        if (context != null) {
            setContext(context)
        }
        // else: Delay until setContext is called.
    }

    fun setContext(context: Context<T?>) {
        if (this.context != null) {
            throw RuntimeException("Cannot replace existing context! This is a bug.")
        }
        this.context = context
        @Suppress("UNCHECKED_CAST")
        context.compile(this as ModuleComponent<T?>)
    }

    fun getContext(): Context<T?>? {
        return context
    }

    val module: T?
        get() = context!!.module

    open fun enabled(): Boolean {
        return context!!.enabled()
    }

    protected abstract fun onEnable()

    protected abstract fun onDisable()

    protected open fun onConfigChange() {}

    internal fun dispatchEnable() = onEnable()
    internal fun dispatchDisable() = onDisable()
    internal fun dispatchConfigChange() = onConfigChange()

    @Throws(IOException::class)
    fun onGenerateResourcePack(pack: ResourcePackGenerator?) {
    }

    fun scheduleTaskTimer(task: Runnable, delayTicks: Long, periodTicks: Long): BukkitTask {
        return context!!.scheduleTaskTimer(task, delayTicks, periodTicks)
    }

    fun scheduleTask(task: Runnable, delayTicks: Long): BukkitTask {
        return context!!.scheduleTask(task, delayTicks)
    }

    fun scheduleNextTick(task: Runnable): BukkitTask {
        return context!!.scheduleNextTick(task)
    }

    fun addStorageMigrationTo(to: Long, name: String?, migrator: Consumer<JSONObject?>) {
        context!!.addStorageMigrationTo(to, name, migrator)
    }

    fun storagePathOf(field: String): String? {
        return context!!.storagePathOf(field)
    }

    fun markPersistentStorageDirty() {
        context!!.markPersistentStorageDirty()
    }

    fun namespacedKey(value: String?): NamespacedKey {
        return NamespacedKey(this.module!!, getContext()!!.variableYamlPath(value)!!)
    }
}
