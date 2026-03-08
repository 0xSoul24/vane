package org.oddlama.vane.regions.menu

import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.Regions

class RegionMenuGroup(context: Context<Regions?>) : ModuleComponent<Regions?>(context.namespace("Menus")) {
    @JvmField
    var enterRegionNameMenu: EnterRegionNameMenu? = EnterRegionNameMenu(getContext()!!)

    @JvmField
    var enterRegionGroupNameMenu: EnterRegionGroupNameMenu? = EnterRegionGroupNameMenu(getContext()!!)

    @JvmField
    var enterRoleNameMenu: EnterRoleNameMenu? = EnterRoleNameMenu(getContext()!!)

    @JvmField
    var mainMenu: MainMenu? = MainMenu(getContext()!!)

    @JvmField
    var regionGroupMenu: RegionGroupMenu? = RegionGroupMenu(getContext()!!)

    @JvmField
    var regionMenu: RegionMenu? = RegionMenu(getContext()!!)

    @JvmField
    var roleMenu: RoleMenu? = RoleMenu(getContext()!!)

    public override fun onEnable() {}

    public override fun onDisable() {}
}
