package org.oddlama.vane.core.menu

import org.bukkit.Material
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent

class HeadSelectorGroup(context: Context<Core?>) :
    ModuleComponent<Core?>(context.namespace("HeadSelector", "Menu configuration for the head selector menu.")) {
    @JvmField
    @LangMessage
    var langTitle: TranslatedMessage? = null

    @JvmField
    @LangMessage
    var langFilterTitle: TranslatedMessage? = null

    @JvmField
    var itemSelectHead: TranslatedItemStack<*>? = TranslatedItemStack<Core?>(
        getContext()!!,
        "SelectHead",
        Material.BARRIER,
        1,
        "Used to represent a head in the head library."
    )

    override fun onEnable() {}

    override fun onDisable() {}
}
