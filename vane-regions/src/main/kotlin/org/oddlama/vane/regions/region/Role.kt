package org.oddlama.vane.regions.region

import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer
import java.io.IOException
import java.util.*

class Role {
    enum class RoleType {
        ADMINS,
        OTHERS,
        NORMAL,
    }

    private var id: UUID? = null
    private var name: String? = null
    private var roleType: RoleType? = null
    private var settings: MutableMap<RoleSetting?, Boolean?>? = EnumMap<RoleSetting, Boolean?>(RoleSetting::class.java)

    private constructor()

    constructor(name: String?, roleType: RoleType?) {
        this.id = UUID.randomUUID()
        this.name = name
        this.roleType = roleType
        for (rs in RoleSetting.entries) {
            this.settings!![rs] = rs.defaultValue(roleType == RoleType.ADMINS)
        }
    }

    fun id(): UUID? {
        return id
    }

    fun name(): String? {
        return name
    }

    fun name(name: String?) {
        this.name = name
    }

    fun roleType(): RoleType? {
        return roleType
    }

    fun settings(): MutableMap<RoleSetting?, Boolean?>? {
        return settings
    }

    fun getSetting(setting: RoleSetting): Boolean {
        if (setting.hasOverride()) {
            return setting.override == 1
        }
        return settings!!.getOrDefault(setting, setting.defaultValue(false))!!
    }

    fun color(): String {
        return when (roleType) {
            RoleType.ADMINS -> "§c"
            RoleType.OTHERS -> "§a"
            RoleType.NORMAL -> "§b"
            else -> "§b"
        }
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun serialize(o: Any): Any {
            val role = o as Role
            val json = JSONObject()
            json.put("id", PersistentSerializer.toJson(UUID::class.java, role.id))
            json.put("name", PersistentSerializer.toJson(String::class.java, role.name))
            json.put("roleType", PersistentSerializer.toJson(RoleType::class.java, role.roleType))
            try {
                json.put(
                    "settings",
                    PersistentSerializer.toJson(Role::class.java.getDeclaredField("settings"), role.settings)
                )
            } catch (e: NoSuchFieldException) {
                throw RuntimeException("Invalid field. This is a bug.", e)
            }

            return json
        }

        @JvmStatic
        @Throws(IOException::class)
        fun deserialize(o: Any): Role {
            val json = o as JSONObject
            val role = Role()
            role.id = PersistentSerializer.fromJson(UUID::class.java, json.get("id"))
            role.name = PersistentSerializer.fromJson(String::class.java, json.get("name"))
            role.roleType = PersistentSerializer.fromJson(RoleType::class.java, json.get("roleType"))
            try {
                role.settings = @Suppress("UNCHECKED_CAST")
                (PersistentSerializer.fromJson(
                    Role::class.java.getDeclaredField("settings"),
                    json.get("settings")
                ) as MutableMap<RoleSetting?, Boolean?>?)
            } catch (e: NoSuchFieldException) {
                throw RuntimeException("Invalid field. This is a bug.", e)
            }
            return role
        }
    }
}
