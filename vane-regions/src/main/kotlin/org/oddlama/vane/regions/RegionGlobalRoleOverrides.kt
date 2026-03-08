package org.oddlama.vane.regions

import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.region.RoleSetting

class RegionGlobalRoleOverrides(context: Context<Regions?>) : ModuleComponent<Regions?>(
    context.namespace(
        "GlobalRoleOverrides",
        "This controls global role setting overrides for all roles in every region on the server. `0` means no-override, the player-configured values are used normally, `1` force-enables this setting for all roles in every region, `-1` force-disables respectively. Force-disable naturally also affects the owner, so be careful!"
    )
) {
    @ConfigInt(
        def = 0,
        min = -1,
        max = 1,
        desc = "Overrides the admin permission. Be careful, this is almost never what you want and may result in immutable regions."
    )
    var configAdmin: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the build permission.")
    var configBuild: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the use permission.")
    var configUse: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the container permission.")
    var configContainer: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the portal permission.")
    var configPortal: Int = 0

    fun getOverride(setting: RoleSetting): Int {
        return when (setting) {
            RoleSetting.ADMIN -> configAdmin
            RoleSetting.BUILD -> configBuild
            RoleSetting.USE -> configUse
            RoleSetting.CONTAINER -> configContainer
            RoleSetting.PORTAL -> configPortal
        }
    }

    public override fun onEnable() {}

    public override fun onDisable() {}
}
