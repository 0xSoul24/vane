package org.oddlama.vane.core.config.loot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.oddlama.vane.core.config.ConfigDictSerializable;

public class LootTableList implements ConfigDictSerializable {

    private List<LootDefinition> tables = new ArrayList<>();

    public List<LootDefinition> tables() {
        return tables;
    }

    // Map special internal names to PascalCase names for YAML generation
    public Map<String, Object> toDict() {
        return tables.stream().collect(Collectors.toMap(t -> toYamlName(t.name()), LootDefinition::serialize));
    }

    public void fromDict(final Map<String, Object> dict) {
        tables.clear();
        for (final Map.Entry<String, Object> e : dict.entrySet()) {
            final String name = e.getKey();
            final var internalName = name == null || name.isEmpty() ? name : fromYamlName(name);
            tables.add(LootDefinition.deserialize(internalName, e.getValue()));
        }
    }

    public static LootTableList of(LootDefinition... defs) {
        final var rl = new LootTableList();
        rl.tables = Arrays.asList(defs);
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
            default:
                return s;
        }
    }
}
