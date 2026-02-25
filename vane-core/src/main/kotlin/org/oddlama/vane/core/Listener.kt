package org.oddlama.vane.core

import org.bukkit.event.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent

open class Listener<T : Module<T?>?>(context: Context<T?>?) : ModuleComponent<T?>(context), Listener {
    override fun onEnable() {
        module!!.registerListener(this)
    }

    override fun onDisable() {
        module!!.unregisterListener(this)
    }
}
