package org.oddlama.vane.regions.region;

import static org.oddlama.vane.core.persistent.PersistentSerializer.fromJson;
import static org.oddlama.vane.core.persistent.PersistentSerializer.toJson;

import java.io.IOException;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.regions.Regions;

public class Region {

    public static Object serialize(@NotNull final Object o) throws IOException {
        final var region = (Region) o;
        final var json = new JSONObject();
        json.put("id", PersistentSerializer.toJson(UUID.class, region.id));
        json.put("name", PersistentSerializer.toJson(String.class, region.name));
        json.put("owner", PersistentSerializer.toJson(UUID.class, region.owner));
        json.put("regionGroup", PersistentSerializer.toJson(UUID.class, region.regionGroup));
        json.put("extent", PersistentSerializer.toJson(RegionExtent.class, region.extent));
        return json;
    }

    @SuppressWarnings("unchecked")
    public static Region deserialize(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var region = new Region();
        region.id = PersistentSerializer.fromJson(UUID.class, json.get("id"));
        region.name = PersistentSerializer.fromJson(String.class, json.get("name"));
        region.owner = PersistentSerializer.fromJson(UUID.class, json.get("owner"));
        region.regionGroup = PersistentSerializer.fromJson(UUID.class, json.get("regionGroup"));
        region.extent = PersistentSerializer.fromJson(RegionExtent.class, json.get("extent"));
        return region;
    }

    private Region() {}

    public Region(final String name, final UUID owner, final RegionExtent extent, final UUID regionGroup) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.owner = owner;
        this.extent = extent;
        this.regionGroup = regionGroup;
    }

    private UUID id;
    private String name;
    private UUID owner;
    private RegionExtent extent;
    private UUID regionGroup;

    public boolean invalidated = true;

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void name(final String name) {
        this.name = name;
        this.invalidated = true;
    }

    public UUID owner() {
        return owner;
    }

    public RegionExtent extent() {
        return extent;
    }

    private RegionGroup cachedRegionGroup = null;

    public UUID regionGroupId() {
        return regionGroup;
    }

    public void regionGroupId(final UUID regionGroup) {
        this.regionGroup = regionGroup;
        this.cachedRegionGroup = null;
        this.invalidated = true;
    }

    public RegionGroup regionGroup(final Regions regions) {
        if (cachedRegionGroup == null) {
            cachedRegionGroup = regions.getRegionGroup(regionGroup);
        }
        return cachedRegionGroup;
    }
}
