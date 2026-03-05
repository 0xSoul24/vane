package org.oddlama.vane.portals.menu

import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.Portals

class PortalMenuGroup(context: Context<Portals?>) : ModuleComponent<Portals?>(context.namespace("Menus")) {
    @JvmField
    var enterNameMenu: EnterNameMenu? = EnterNameMenu(getContext()!!)

    @JvmField
    var consoleMenu: ConsoleMenu? = ConsoleMenu(getContext()!!)

    @JvmField
    var settingsMenu: SettingsMenu? = SettingsMenu(getContext()!!)

    @JvmField
    var styleMenu: StyleMenu? = StyleMenu(getContext()!!)

    public override fun onEnable() {}

    public override fun onDisable() {}
}
