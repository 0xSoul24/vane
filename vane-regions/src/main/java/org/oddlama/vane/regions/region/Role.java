package org.oddlama.vane.regions.region;

import static org.oddlama.vane.core.persistent.PersistentSerializer.fromJson;
import static org.oddlama.vane.core.persistent.PersistentSerializer.toJson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.oddlama.vane.core.persistent.PersistentSerializer;

public class Role {

    public enum RoleType {
        ADMINS,
        OTHERS,
        NORMAL,
    }

    public static Object serialize(@NotNull final Object o) throws IOException {
        final var role = (Role) o;
        final var json = new JSONObject();
        json.put("id", PersistentSerializer.toJson(UUID.class, role.id));
        json.put("name", PersistentSerializer.toJson(String.class, role.name));
        json.put("roleType", PersistentSerializer.toJson(RoleType.class, role.roleType));
        try {
            json.put("settings", PersistentSerializer.toJson(Role.class.getDeclaredField("settings"), role.settings));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }

        return json;
    }

    @SuppressWarnings("unchecked")
    public static Role deserialize(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var role = new Role();
        role.id = PersistentSerializer.fromJson(UUID.class, json.get("id"));
        role.name = PersistentSerializer.fromJson(String.class, json.get("name"));
        role.roleType = PersistentSerializer.fromJson(RoleType.class, json.get("roleType"));
        try {
            role.settings = (Map<RoleSetting, Boolean>) PersistentSerializer.fromJson(
                Role.class.getDeclaredField("settings"),
                json.get("settings")
            );
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }
        return role;
    }

    private UUID id;
    private String name;
    private RoleType roleType;
    private Map<RoleSetting, Boolean> settings = new HashMap<>();

    private Role() {}

    public Role(final String name, final RoleType roleType) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.roleType = roleType;
        for (final var rs : RoleSetting.values()) {
            this.settings.put(rs, rs.defaultValue(roleType == RoleType.ADMINS));
        }
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void name(final String name) {
        this.name = name;
    }

    public RoleType roleType() {
        return roleType;
    }

    public Map<RoleSetting, Boolean> settings() {
        return settings;
    }

    public boolean getSetting(final RoleSetting setting) {
        if (setting.hasOverride()) {
            return setting.getOverride() == 1;
        }
        return settings.getOrDefault(setting, setting.defaultValue(false));
    }

    public String color() {
        switch (roleType) {
            case ADMINS:
                return "§c";
            case OTHERS:
                return "§a";
            default:
            case NORMAL:
                return "§b";
        }
    }
}
