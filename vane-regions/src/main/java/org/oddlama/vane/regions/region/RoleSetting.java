package org.oddlama.vane.regions.region;

import org.oddlama.vane.regions.Regions;

public enum RoleSetting {
    ADMIN(false, true),
    BUILD(false, true),
    USE(true, true),
    CONTAINER(false, true),
    PORTAL(false, true);

    private boolean def;
    private boolean defAdmin;

    private RoleSetting(final boolean def, final boolean defAdmin) {
        this.def = def;
        this.defAdmin = defAdmin;
    }

    public boolean defaultValue(final boolean admin) {
        if (admin) {
            return defAdmin;
        }
        return def;
    }

    public boolean hasOverride() {
        return getOverride() != 0;
    }

    public int getOverride() {
        return Regions.roleOverrides.getOverride(this);
    }
}
