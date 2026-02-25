package org.oddlama.vane.core.module

import org.oddlama.vane.annotation.config.ConfigBoolean

/**
 * A ModuleGroup is a ModuleContext that automatically adds an enabled variable with description to
 * the context. If the group is disabled, onModuleEnable() will not be called.
 */
open class ModuleGroup<T : Module<T?>?> @JvmOverloads constructor(
    context: Context<T?>,
    group: String?,
    private val configEnabledDesc: String?,
    compileSelf: Boolean = true
) : ModuleContext<T?>(context, group, null, ".", false) {
    @ConfigBoolean(def = true, desc = "") // desc is set by #configEnabledDesc()
    var configEnabled: Boolean = false

    @JvmField
    var configEnabledDef: Boolean = true

    fun configEnabledDef(): Boolean {
        return configEnabledDef
    }

    fun configEnabledDesc(): String? {
        return configEnabledDesc
    }

    init {
        if (compileSelf) {
            compileSelf()
        }
    }

    override fun enabled(): Boolean {
        return configEnabled
    }

    override fun enable() {
        if (configEnabled) {
            super.enable()
        }
    }

    override fun disable() {
        if (configEnabled) {
            super.disable()
        }
    }

    companion object {
        @JvmStatic
        fun <U : Module<U?>?> create(context: Context<U?>, group: String?, description: String?): ModuleGroup<U?> {
            return ModuleGroup(context, group, description)
        }
    }
}
