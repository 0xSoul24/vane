package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigStringListMap;
import org.oddlama.vane.annotation.config.ConfigStringListMapEntry;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigStringListMapField extends ConfigField<Map<String, List<String>>> {

    public ConfigStringListMap annotation;

    public ConfigStringListMapField(
        Object owner,
        Field field,
        Function<String, String> mapName,
        ConfigStringListMap annotation
    ) {
        super(owner, field, mapName, "map of string to string list", annotation.desc());
        this.annotation = annotation;
    }

    // Convert a key like "default" or "terralith_rare" into PascalCase: "Default" / "TerralithRare"
    private static String toPascalCase(final String key) {
        if (key == null || key.isEmpty()) return key;
        final var parts = key.split("[^A-Za-z0-9]+");
        final var sb = new StringBuilder();
        for (final var p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
        }
        return sb.toString();
    }

    // Normalize keys from YAML back to canonical internal form (lowercase, non-alphanum -> underscore)
    private static String normalizeKey(final String key) {
        if (key == null) return null;
        return key.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private void appendStringListMapDefinition(
        StringBuilder builder,
        String indent,
        String prefix,
        Map<String, List<String>> def
    ) {
        def.forEach((k, list) -> {
            builder.append(indent);
            builder.append(prefix);
            builder.append("  ");
            // Use PascalCase for keys in generated YAML
            builder.append(escapeYaml(toPascalCase(k)));
            builder.append(":\n");

            list.forEach(s -> {
                builder.append(indent);
                builder.append(prefix);
                builder.append("    - ");
                builder.append(escapeYaml(s));
                builder.append("\n");
            });
        });
    }

    @Override
    public Map<String, List<String>> def() {
        final var override = overriddenDef();
        if (override != null) {
            return override;
        } else {
            return Arrays.stream(annotation.def()).collect(
                Collectors.toMap(ConfigStringListMapEntry::key, e -> Arrays.asList(e.list()))
            );
        }
    }

    @Override
    public boolean metrics() {
        final var override = overriddenMetrics();
        if (override != null) {
            return override;
        } else {
            return annotation.metrics();
        }
    }

    @Override
    public void generateYaml(StringBuilder builder, String indent, YamlConfiguration existingCompatibleConfig) {
        appendDescription(builder, indent);

        // Default
        builder.append(indent);
        builder.append("# Default:\n");
        appendStringListMapDefinition(builder, indent, "# ", def());

        // Definition
        builder.append(indent);
        builder.append(basename());
        builder.append(":\n");
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendStringListMapDefinition(builder, indent, "", def);
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isConfigurationSection(yamlPath())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected group");
        }

        for (var listKey : yaml.getConfigurationSection(yamlPath()).getKeys(false)) {
            final var listPath = yamlPath() + "." + listKey;
            if (!yaml.isList(listPath)) {
                throw new YamlLoadException("Invalid type for yaml path '" + listPath + "', expected list");
            }

            for (var obj : yaml.getList(listPath)) {
                if (!(obj instanceof String)) {
                    throw new YamlLoadException("Invalid type for yaml path '" + listPath + "', expected string");
                }
            }
        }
    }

    public Map<String, List<String>> loadFromYaml(YamlConfiguration yaml) {
        final var map = new HashMap<String, List<String>>();
        for (final var listKey : yaml.getConfigurationSection(yamlPath()).getKeys(false)) {
            final var listPath = yamlPath() + "." + listKey;
            final var list = new ArrayList<String>();
            // Normalize keys so in-memory representation stays lowercase/underscore
            map.put(normalizeKey(listKey), list);
            for (final var obj : yaml.getList(listPath)) {
                list.add((String) obj);
            }
        }
        return map;
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, loadFromYaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
