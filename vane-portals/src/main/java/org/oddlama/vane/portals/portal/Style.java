package org.oddlama.vane.portals.portal;

import static org.oddlama.vane.core.persistent.PersistentSerializer.fromJson;
import static org.oddlama.vane.core.persistent.PersistentSerializer.toJson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.util.StorageUtil;

public class Style {

    public static Object serialize(@NotNull final Object o) throws IOException {
        final var style = (Style) o;
        final var json = new JSONObject();
        json.put("key", PersistentSerializer.toJson(NamespacedKey.class, style.key));
        try {
            json.put(
                "activeMaterials",
                PersistentSerializer.toJson(Style.class.getDeclaredField("activeMaterials"), style.activeMaterials)
            );
            json.put(
                "inactiveMaterials",
                PersistentSerializer.toJson(Style.class.getDeclaredField("inactiveMaterials"), style.inactiveMaterials)
            );
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }
        return json;
    }

    @SuppressWarnings("unchecked")
    public static Style deserialize(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var style = new Style(null);
        style.key = PersistentSerializer.fromJson(NamespacedKey.class, json.get("key"));
        try {
            style.activeMaterials = (Map<PortalBlock.Type, Material>) PersistentSerializer.fromJson(
                Style.class.getDeclaredField("activeMaterials"),
                json.get("activeMaterials")
            );
            style.inactiveMaterials = (Map<PortalBlock.Type, Material>) PersistentSerializer.fromJson(
                Style.class.getDeclaredField("inactiveMaterials"),
                json.get("inactiveMaterials")
            );
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }
        return style;
    }

    private NamespacedKey key;
    private Map<PortalBlock.Type, Material> activeMaterials = new HashMap<>();
    private Map<PortalBlock.Type, Material> inactiveMaterials = new HashMap<>();

    public Style(final NamespacedKey key) {
        this.key = key;
    }

    public NamespacedKey key() {
        return key;
    }

    public Material material(boolean active, PortalBlock.Type type) {
        if (active) {
            return activeMaterials.get(type);
        } else {
            return inactiveMaterials.get(type);
        }
    }

    public static NamespacedKey defaultStyleKey() {
        return StorageUtil.namespacedKey("vane_portals", "portal_style_default");
    }

    public void setMaterial(boolean active, PortalBlock.Type type, Material material) {
        setMaterial(active, type, material, false);
    }

    public void setMaterial(boolean active, PortalBlock.Type type, Material material, boolean overwrite) {
        final Map<PortalBlock.Type, Material> map;
        if (active) {
            map = activeMaterials;
        } else {
            map = inactiveMaterials;
        }

        if (!overwrite && map.containsKey(type)) {
            throw new RuntimeException(
                "Invalid style definition! PortalBlock.Type." + type + " was specified multiple times."
            );
        }
        map.put(type, material);
    }

    public void checkValid() {
        // Checks if every key is set
        for (final var type : PortalBlock.Type.values()) {
            if (!activeMaterials.containsKey(type)) {
                throw new RuntimeException(
                    "Invalid style definition! Active state for PortalBlock.Type." + type + " was not specified!"
                );
            }
            if (!inactiveMaterials.containsKey(type)) {
                throw new RuntimeException(
                    "Invalid style definition! Inactive state for PortalBlock.Type." + type + " was not specified!"
                );
            }
        }
    }

    public static Style defaultStyle() {
        final var style = new Style(defaultStyleKey());
        style.setMaterial(true, PortalBlock.Type.BOUNDARY1, Material.OBSIDIAN);
        style.setMaterial(true, PortalBlock.Type.BOUNDARY2, Material.CRYING_OBSIDIAN);
        style.setMaterial(true, PortalBlock.Type.BOUNDARY3, Material.GOLD_BLOCK);
        style.setMaterial(true, PortalBlock.Type.BOUNDARY4, Material.GILDED_BLACKSTONE);
        style.setMaterial(true, PortalBlock.Type.BOUNDARY5, Material.EMERALD_BLOCK);
        style.setMaterial(true, PortalBlock.Type.CONSOLE, Material.ENCHANTING_TABLE);
        style.setMaterial(true, PortalBlock.Type.ORIGIN, Material.OBSIDIAN);
        style.setMaterial(true, PortalBlock.Type.PORTAL, Material.END_GATEWAY);
        style.setMaterial(false, PortalBlock.Type.BOUNDARY1, Material.OBSIDIAN);
        style.setMaterial(false, PortalBlock.Type.BOUNDARY2, Material.CRYING_OBSIDIAN);
        style.setMaterial(false, PortalBlock.Type.BOUNDARY3, Material.GOLD_BLOCK);
        style.setMaterial(false, PortalBlock.Type.BOUNDARY4, Material.GILDED_BLACKSTONE);
        style.setMaterial(false, PortalBlock.Type.BOUNDARY5, Material.EMERALD_BLOCK);
        style.setMaterial(false, PortalBlock.Type.CONSOLE, Material.ENCHANTING_TABLE);
        style.setMaterial(false, PortalBlock.Type.ORIGIN, Material.OBSIDIAN);
        style.setMaterial(false, PortalBlock.Type.PORTAL, Material.AIR);
        return style;
    }

    public Style copy(final NamespacedKey newKey) {
        final var copy = new Style(newKey);
        copy.activeMaterials = new HashMap<>(activeMaterials);
        copy.inactiveMaterials = new HashMap<>(inactiveMaterials);
        return copy;
    }
}
