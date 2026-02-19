package org.oddlama.vane.core.config.recipes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.oddlama.vane.core.config.ConfigDictSerializable;

public class RecipeList implements ConfigDictSerializable {

    private List<RecipeDefinition> recipes = new ArrayList<>();

    public RecipeList() {}

    public RecipeList(List<RecipeDefinition> recipes) {
        this.recipes = recipes;
    }

    public List<RecipeDefinition> recipes() {
        return recipes;
    }

    // Mapear nombres especiales a PascalCase para YAML: 'generic' -> 'Generic', 'terralith_generic' -> 'TerralithGeneric', 'terralith_rare' -> 'TerralithRare', 'ancientcity' -> 'AncientCity', 'bastion' -> 'Bastion', 'from_shulker_box' -> 'FromShulkerBox'
    public Map<String, Object> toDict() {
        return recipes
            .stream()
            .collect(Collectors.toMap(r -> toYamlName(r.name()), RecipeDefinition::toDict));
    }

    public void fromDict(final Map<String, Object> dict) {
        recipes.clear();
        for (final var e : dict.entrySet()) {
            final var key = e.getKey();
            final String name = key == null ? null : key;
            final var internalName = name == null || name.isEmpty() ? name : fromYamlName(name);
            recipes.add(RecipeDefinition.fromDict(internalName, e.getValue()));
        }
    }

    public static RecipeList of(RecipeDefinition... defs) {
        final var rl = new RecipeList();
        rl.recipes = Arrays.asList(defs);
        return rl;
    }

    private static String toYamlName(final String s) {
        if (s == null || s.isEmpty()) return s;
        final var low = s.toLowerCase();
        switch (low) {
            case "generic":
                return "Generic";
            case "terralith_generic":
                return "TerralithGeneric";
            case "terralith_rare":
                return "TerralithRare";
            case "ancientcity":
                return "AncientCity";
            case "bastion":
                return "Bastion";
            case "from_shulker_box":
                return "FromShulkerBox";
            default:
                return s;
        }
    }

    private static String fromYamlName(final String s) {
        if (s == null || s.isEmpty()) return s;
        final var low = s.toLowerCase().replaceAll("[_\\s]", "");
        switch (low) {
            case "generic":
                return "generic";
            case "terralithgeneric":
                return "terralith_generic";
            case "terralithrare":
                return "terralith_rare";
            case "ancientcity":
                return "ancientcity";
            case "bastion":
                return "bastion";
            case "fromshulkerbox":
                return "from_shulker_box";
            default:
                return s;
        }
    }
}
