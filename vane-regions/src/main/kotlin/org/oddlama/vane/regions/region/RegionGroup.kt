package org.oddlama.vane.regions.region

import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.regions.Regions
import java.io.IOException
import java.util.*

class RegionGroup {
    private var id: UUID? = null
    private var name: String? = null
    private var owner: UUID? = null

    private var roles: MutableMap<UUID?, Role?>? = HashMap<UUID?, Role?>()
    private var playerToRole: MutableMap<UUID?, UUID?>? = HashMap<UUID?, UUID?>()
    private var roleOthers: UUID? = null

    private var settings: MutableMap<EnvironmentSetting?, Boolean?>? = EnumMap<EnvironmentSetting, Boolean?>(EnvironmentSetting::class.java)

    private constructor()

    constructor(name: String?, owner: UUID?) {
        this.id = UUID.randomUUID()
        this.name = name
        this.owner = owner

        // Add admins role
        val admins = Role("[Admins]", Role.RoleType.ADMINS)
        this.addRole(admins)

        // Add another role
        val others = Role("[Others]", Role.RoleType.OTHERS)
        this.addRole(others)
        this.roleOthers = others.id()

        // Add "friends" role
        val friends = Role("Friends", Role.RoleType.NORMAL)
        friends.settings()!![RoleSetting.BUILD] = true
        friends.settings()!![RoleSetting.USE] = true
        friends.settings()!![RoleSetting.CONTAINER] = true
        friends.settings()!![RoleSetting.PORTAL] = true
        this.addRole(friends)

        // Add "owner" to admins
        this.playerToRole!![owner] = admins.id()

        // Set setting defaults
        for (es in EnvironmentSetting.entries) {
            this.settings!![es] = es.defaultValue()
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

    fun owner(): UUID? {
        return owner
    }

    fun settings(): MutableMap<EnvironmentSetting?, Boolean?>? {
        return settings
    }

    fun getSetting(setting: EnvironmentSetting): Boolean {
        if (setting.hasOverride()) {
            return setting.override == 1
        }
        return settings!!.getOrDefault(setting, setting.defaultValue())!!
    }

    fun addRole(role: Role) {
        this.roles!![role.id()] = role
    }

    fun playerToRole(): MutableMap<UUID?, UUID?>? {
        return playerToRole
    }

    fun getRole(player: UUID?): Role? {
        return roles!![playerToRole!!.getOrDefault(player, roleOthers)]
    }

    fun removeRole(roleId: UUID) {
        playerToRole!!.values.removeIf { r: UUID? -> roleId == r }
        roles!!.remove(roleId)
    }

    fun roles(): MutableCollection<Role?> {
        return roles!!.values
    }

    fun isOrphan(regions: Regions): Boolean {
        return !regions.allRegions().stream().anyMatch { r: Region? -> id == r!!.regionGroupId() }
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun serialize(o: Any): Any {
            val regionGroup = o as RegionGroup
            val json = JSONObject()
            putOwnable(json, regionGroup.id, regionGroup.name, regionGroup.owner)
            withField { json.put("roles", PersistentSerializer.toJson(RegionGroup::class.java.getDeclaredField("roles"), regionGroup.roles)) }
            withField { json.put("playerToRole", PersistentSerializer.toJson(RegionGroup::class.java.getDeclaredField("playerToRole"), regionGroup.playerToRole)) }
            json.put("roleOthers", PersistentSerializer.toJson(UUID::class.java, regionGroup.roleOthers))
            withField { json.put("settings", PersistentSerializer.toJson(RegionGroup::class.java.getDeclaredField("settings"), regionGroup.settings)) }
            return json
        }

        @JvmStatic
        @Throws(IOException::class)
        fun deserialize(o: Any): RegionGroup {
            val json = o as JSONObject
            val regionGroup = RegionGroup()
            val (id, name, owner) = readOwnable(json)
            regionGroup.id = id
            regionGroup.name = name
            regionGroup.owner = owner
            withField {
                @Suppress("UNCHECKED_CAST")
                regionGroup.roles = PersistentSerializer.fromJson(RegionGroup::class.java.getDeclaredField("roles"), json.get("roles")) as MutableMap<UUID?, Role?>?
            }
            withField {
                @Suppress("UNCHECKED_CAST")
                regionGroup.playerToRole = PersistentSerializer.fromJson(RegionGroup::class.java.getDeclaredField("playerToRole"), json.get("playerToRole")) as MutableMap<UUID?, UUID?>?
            }
            regionGroup.roleOthers = PersistentSerializer.fromJson(UUID::class.java, json.get("roleOthers"))
            withField {
                @Suppress("UNCHECKED_CAST")
                regionGroup.settings = PersistentSerializer.fromJson(RegionGroup::class.java.getDeclaredField("settings"), json.get("settings")) as MutableMap<EnvironmentSetting?, Boolean?>?
            }
            return regionGroup
        }
    }
}
