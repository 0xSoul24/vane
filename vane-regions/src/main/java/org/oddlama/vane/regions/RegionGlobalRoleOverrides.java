package org.oddlama.vane.regions;

import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.regions.region.RoleSetting;

public class RegionGlobalRoleOverrides extends ModuleComponent<Regions> {

    @ConfigInt(
        def = 0,
        min = -1,
        max = 1,
        desc = "Overrides the admin permission. Be careful, this is almost never what you want and may result in immutable regions."
    )
    public int configAdmin;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the build permission.")
    public int configBuild;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the use permission.")
    public int configUse;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the container permission.")
    public int configContainer;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the portal permission.")
    public int configPortal;

    public RegionGlobalRoleOverrides(Context<Regions> context) {
        super(
            context.namespace(
                "GlobalRoleOverrides",
                "This controls global role setting overrides for all roles in every region on the server. `0` means no-override, the player-configured values are used normally, `1` force-enables this setting for all roles in every region, `-1` force-disables respectively. Force-disable naturally also affects the owner, so be careful!"
            )
        );
    }

    public int getOverride(final RoleSetting setting) {
        switch (setting) {
            case ADMIN:
                return configAdmin;
            case BUILD:
                return configBuild;
            case USE:
                return configUse;
            case CONTAINER:
                return configContainer;
            case PORTAL:
                return configPortal;
        }
        return 0;
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
