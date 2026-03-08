package org.oddlama.vane.regions.region

import org.oddlama.vane.regions.Regions

enum class RoleSetting(private val def: Boolean, private val defAdmin: Boolean) {
    ADMIN(false, true),
    BUILD(false, true),
    USE(true, true),
    CONTAINER(false, true),
    PORTAL(false, true);

    fun defaultValue(admin: Boolean): Boolean {
        if (admin) {
            return defAdmin
        }
        return def
    }

    fun hasOverride(): Boolean {
        return this.override != 0
    }

    val override: Int
        get() = Regions.roleOverrides?.getOverride(this) ?: 0
}
