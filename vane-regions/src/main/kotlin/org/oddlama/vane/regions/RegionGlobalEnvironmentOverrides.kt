package org.oddlama.vane.regions

import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.region.EnvironmentSetting

class RegionGlobalEnvironmentOverrides(context: Context<Regions?>) : ModuleComponent<Regions?>(
    context.namespace(
        "GlobalEnvironmentOverrides",
        "This controls global environment setting overrides for all regions on the server. `0` means no-override, the player-configured values are used normally, `1` force-enables this setting for all regions, `-1` force-disables respectively."
    )
) {
    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether animals can spawn.")
    var configAnimals: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether monsters can spawn.")
    var configMonsters: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether explosions can happen.")
    var configExplosions: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether fire spreads and consumes.")
    var configFire: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether pvp is allowed.")
    var configPvp: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether fields can be trampled.")
    var configTrample: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether vines can grow.")
    var configVineGrowth: Int = 0

    fun getOverride(setting: EnvironmentSetting): Int {
        return when (setting) {
            EnvironmentSetting.ANIMALS -> configAnimals
            EnvironmentSetting.MONSTERS -> configMonsters
            EnvironmentSetting.EXPLOSIONS -> configExplosions
            EnvironmentSetting.FIRE -> configFire
            EnvironmentSetting.PVP -> configPvp
            EnvironmentSetting.TRAMPLE -> configTrample
            EnvironmentSetting.VINE_GROWTH -> configVineGrowth
        }
    }

    public override fun onEnable() {}

    public override fun onDisable() {}
}
