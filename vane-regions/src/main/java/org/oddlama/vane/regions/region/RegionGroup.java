package org.oddlama.vane.regions.region;

import static org.oddlama.vane.core.persistent.PersistentSerializer.fromJson;
import static org.oddlama.vane.core.persistent.PersistentSerializer.toJson;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.regions.Regions;

public class RegionGroup {

    public static Object serialize(@NotNull final Object o) throws IOException {
        final var regionGroup = (RegionGroup) o;
        final var json = new JSONObject();
        json.put("id", PersistentSerializer.toJson(UUID.class, regionGroup.id));
        json.put("name", PersistentSerializer.toJson(String.class, regionGroup.name));
        json.put("owner", PersistentSerializer.toJson(UUID.class, regionGroup.owner));
        try {
            json.put("roles", PersistentSerializer.toJson(RegionGroup.class.getDeclaredField("roles"), regionGroup.roles));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }
        try {
            json.put(
                "playerToRole",
                PersistentSerializer.toJson(RegionGroup.class.getDeclaredField("playerToRole"), regionGroup.playerToRole)
            );
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }
        json.put("roleOthers", PersistentSerializer.toJson(UUID.class, regionGroup.roleOthers));
        try {
            json.put("settings", PersistentSerializer.toJson(RegionGroup.class.getDeclaredField("settings"), regionGroup.settings));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }

        return json;
    }

    @SuppressWarnings("unchecked")
    public static RegionGroup deserialize(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var regionGroup = new RegionGroup();
        regionGroup.id = PersistentSerializer.fromJson(UUID.class, json.get("id"));
        regionGroup.name = PersistentSerializer.fromJson(String.class, json.get("name"));
        regionGroup.owner = PersistentSerializer.fromJson(UUID.class, json.get("owner"));
        try {
            regionGroup.roles = (Map<UUID, Role>) PersistentSerializer.fromJson(
                RegionGroup.class.getDeclaredField("roles"),
                json.get("roles")
            );
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }
        try {
            regionGroup.playerToRole = (Map<UUID, UUID>) PersistentSerializer.fromJson(
                RegionGroup.class.getDeclaredField("playerToRole"),
                json.get("playerToRole")
            );
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }
        regionGroup.roleOthers = PersistentSerializer.fromJson(UUID.class, json.get("roleOthers"));
        try {
            regionGroup.settings = (Map<EnvironmentSetting, Boolean>) PersistentSerializer.fromJson(
                RegionGroup.class.getDeclaredField("settings"),
                json.get("settings")
            );
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }
        return regionGroup;
    }

    private UUID id;
    private String name;
    private UUID owner;

    private Map<UUID, Role> roles = new HashMap<>();
    private Map<UUID, UUID> playerToRole = new HashMap<>();
    private UUID roleOthers;

    private Map<EnvironmentSetting, Boolean> settings = new HashMap<>();

    private RegionGroup() {}

    public RegionGroup(final String name, final UUID owner) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.owner = owner;

        // Add admins role
        final var admins = new Role("[Admins]", Role.RoleType.ADMINS);
        this.addRole(admins);

        // Add another role
        final var others = new Role("[Others]", Role.RoleType.OTHERS);
        this.addRole(others);
        this.roleOthers = others.id();

        // Add "friends" role
        final var friends = new Role("Friends", Role.RoleType.NORMAL);
        friends.settings().put(RoleSetting.BUILD, true);
        friends.settings().put(RoleSetting.USE, true);
        friends.settings().put(RoleSetting.CONTAINER, true);
        friends.settings().put(RoleSetting.PORTAL, true);
        this.addRole(friends);

        // Add "owner" to admins
        this.playerToRole.put(owner, admins.id());

        // Set setting defaults
        for (var es : EnvironmentSetting.values()) {
            this.settings.put(es, es.defaultValue());
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

    public UUID owner() {
        return owner;
    }

    public Map<EnvironmentSetting, Boolean> settings() {
        return settings;
    }

    public boolean getSetting(final EnvironmentSetting setting) {
        if (setting.hasOverride()) {
            return setting.getOverride() == 1;
        }
        return settings.getOrDefault(setting, setting.defaultValue());
    }

    public void addRole(final Role role) {
        this.roles.put(role.id(), role);
    }

    public Map<UUID, UUID> playerToRole() {
        return playerToRole;
    }

    public Role getRole(final UUID player) {
        return roles.get(playerToRole.getOrDefault(player, roleOthers));
    }

    public void removeRole(final UUID roleId) {
        playerToRole.values().removeIf(r -> roleId.equals(r));
        roles.remove(roleId);
    }

    public Collection<Role> roles() {
        return roles.values();
    }

    public boolean isOrphan(final Regions regions) {
        return !regions.allRegions().stream().anyMatch(r -> id.equals(r.regionGroupId()));
    }
}
