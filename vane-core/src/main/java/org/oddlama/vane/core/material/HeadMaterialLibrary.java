package org.oddlama.vane.core.material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.json.JSONArray;

public class HeadMaterialLibrary {

    private static final Map<NamespacedKey, HeadMaterial> registry = new HashMap<>();
    private static final Map<String, List<HeadMaterial>> categories = new HashMap<>();
    private static final Map<String, List<HeadMaterial>> tags = new HashMap<>();
    private static final Map<String, HeadMaterial> byTexture = new HashMap<>();

    public static void load(final String string) {
        final var json = new JSONArray(string);
        for (int i = 0; i < json.length(); ++i) {
            // Deserialize
            final var mat = HeadMaterial.from(json.getJSONObject(i));

            // Add to registry
            registry.put(mat.key(), mat);
            byTexture.put(mat.texture(), mat);

            // Add to category lookup
            var category = categories.computeIfAbsent(mat.category(), k -> new ArrayList<>());
            category.add(mat);

            // Add to tag lookup
            for (final var tag : mat.tags()) {
                var tagList = tags.computeIfAbsent(tag, k -> new ArrayList<>());
                tagList.add(mat);
            }
        }
    }

    public static HeadMaterial from(final NamespacedKey key) {
        return registry.get(key);
    }

    public static HeadMaterial fromTexture(final String base64Texture) {
        return byTexture.get(base64Texture);
    }

    public static Collection<HeadMaterial> all() {
        return registry.values();
    }
}
