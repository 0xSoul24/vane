package org.oddlama.vane.core.module

import org.bukkit.scheduler.BukkitTask
import org.json.JSONObject
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import java.io.IOException
import java.util.function.Consumer

/**
 * A ModuleContext is an association to a specific Module and also a grouping of config and language
 * variables with a common namespace.
 */
interface Context<T : Module<T?>?> {
    /** create a sub-context namespace  */
    /** create a sub-context namespace  */
    @Suppress("UNCHECKED_CAST")
    fun namespace(name: String?): ModuleContext<T?> {
        val ctx: Context<T?> = this as Context<T?>
        return ModuleContext(ctx, name, null, ".")
    }

    /** create a sub-context namespace  */
    @Suppress("UNCHECKED_CAST")
    fun namespace(name: String?, description: String?): ModuleContext<T?> {
        val ctx: Context<T?> = this as Context<T?>
        return ModuleContext(ctx, name, description, ".")
    }

    /** create a sub-context namespace  */
    @Suppress("UNCHECKED_CAST")
    fun namespace(name: String?, description: String?, separator: String?): ModuleContext<T?> {
        val ctx: Context<T?> = this as Context<T?>
        return ModuleContext(ctx, name, description, separator)
    }

    /** create a sub-context group  */
    @Suppress("UNCHECKED_CAST")
    fun group(group: String?, description: String?): ModuleGroup<T?> {
        val ctx: Context<T?> = this as Context<T?>
        return ModuleGroup.create(ctx, group, description)
    }

    @Suppress("UNCHECKED_CAST")
    fun group(group: String?, description: String?, defaultEnabled: Boolean): ModuleGroup<T?> {
        val ctx: Context<T?> = this as Context<T?>
        val g = ModuleGroup.create(ctx, group, description)
        g.configEnabledDef = defaultEnabled
        return g
    }

    /** create a sub-context group  */
    fun groupDefaultDisabled(group: String?, description: String?): ModuleGroup<T?> {
        val g = group(group, description)
        g.configEnabledDef = false
        return g
    }

    /**
     * Compile the given component (processes lang and config definitions) and registers it for
     * onModuleEnable, onModuleDisable and onConfigChange events.
     */
    fun compile(component: ModuleComponent<T?>?)

    fun addChild(subcontext: Context<T?>?)

    val context: Context<T?>?

    val module: T?

    fun yamlPath(): String?

    fun variableYamlPath(variable: String?): String?

    fun enabled(): Boolean

    fun enable()

    fun disable()

    fun configChange()

    @Throws(IOException::class)
    fun generateResourcePack(pack: ResourcePackGenerator?)

    fun forEachModuleComponent(f: Consumer1<ModuleComponent<*>?>?)

    fun onModuleEnable() {}

    fun onModuleDisable() {}

    fun onConfigChange() {}

    @Throws(IOException::class)
    fun onGenerateResourcePack(pack: ResourcePackGenerator?) {
    }

    fun scheduleTaskTimer(task: Runnable, delayTicks: Long, periodTicks: Long): BukkitTask {
        return this.module!!.server.scheduler.runTaskTimer(this.module!!, task, delayTicks, periodTicks)
    }

    fun scheduleTask(task: Runnable, delayTicks: Long): BukkitTask {
        return this.module!!.server.scheduler.runTaskLater(this.module!!, task, delayTicks)
    }

    fun scheduleNextTick(task: Runnable): BukkitTask {
        return this.module!!.server.scheduler.runTask(this.module!!, task)
    }

    fun addStorageMigrationTo(to: Long, name: String?, migrator: Consumer<JSONObject?>) {
        this.module!!.persistentStorageManager.addMigrationTo(to, name, migrator)
    }

    fun storagePathOf(field: String): String? {
        if (!field.startsWith("storage")) {
            throw RuntimeException("Configuration fields must be prefixed storage. This is a bug.")
        }
        return variableYamlPath(field.substring("storage".length))
    }

    fun markPersistentStorageDirty() {
        this.module!!.markPersistentStorageDirty()
    }

    companion object {
        @JvmStatic
        fun appendYamlPath(ns1: String, ns2: String?, separator: String?): String? {
            if (ns1.isEmpty()) {
                return ns2
            }
            return ns1 + separator + ns2
        }
    }
}
