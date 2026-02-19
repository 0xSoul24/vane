package org.oddlama.vane.regions;

import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.regions.region.EnvironmentSetting;

public class RegionGlobalEnvironmentOverrides extends ModuleComponent<Regions> {

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether animals can spawn.")
    public int configAnimals;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether monsters can spawn.")
    public int configMonsters;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether explosions can happen.")
    public int configExplosions;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether fire spreads and consumes.")
    public int configFire;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether pvp is allowed.")
    public int configPvp;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether fields can be trampled.")
    public int configTrample;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether vines can grow.")
    public int configVineGrowth;

    public RegionGlobalEnvironmentOverrides(Context<Regions> context) {
        super(
            context.namespace(
                "GlobalEnvironmentOverrides",
                "This controls global environment setting overrides for all regions on the server. `0` means no-override, the player-configured values are used normally, `1` force-enables this setting for all regions, `-1` force-disables respectively."
            )
        );
    }

    public int getOverride(final EnvironmentSetting setting) {
        switch (setting) {
            case ANIMALS:
                return configAnimals;
            case MONSTERS:
                return configMonsters;
            case EXPLOSIONS:
                return configExplosions;
            case FIRE:
                return configFire;
            case PVP:
                return configPvp;
            case TRAMPLE:
                return configTrample;
            case VINE_GROWTH:
                return configVineGrowth;
        }
        return 0;
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
