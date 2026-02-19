package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.MaterialUtil.materialFrom;
import static org.oddlama.vane.util.StorageUtil.namespacedKey;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigMaterialMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMapEntry;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigMaterialMapMapMapField extends ConfigField<Map<String, Map<String, Map<String, Material>>>> {

    public ConfigMaterialMapMapMap annotation;

    public ConfigMaterialMapMapMapField(
        Object owner,
        Field field,
        Function<String, String> mapName,
        ConfigMaterialMapMapMap annotation
    ) {
        super(
            owner,
            field,
            mapName,
            "map of string to (map of string to (map of string to material))",
            annotation.desc()
        );
        this.annotation = annotation;
    }

    private void appendMapDefinition(
        StringBuilder builder,
        String indent,
        String prefix,
        Map<String, Map<String, Map<String, Material>>> def
    ) {
        def
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e1 -> {
                builder.append(indent);
                builder.append(prefix);
                builder.append("  ");
                builder.append(escapeYaml(e1.getKey()));
                builder.append(":\n");

                e1
                    .getValue()
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e2 -> {
                        builder.append(indent);
                        builder.append(prefix);
                        builder.append("    ");
                        builder.append(escapeYaml(e2.getKey()));
                        builder.append(":\n");

                        e2
                            .getValue()
                            .entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(e3 -> {
                                builder.append(indent);
                                builder.append(prefix);
                                builder.append("      ");
                                builder.append(escapeYaml(e3.getKey()));
                                builder.append(": \"");
                                builder.append(escapeYaml(e3.getValue().getKey().getNamespace()));
                                builder.append(":");
                                builder.append(escapeYaml(e3.getValue().getKey().getKey()));
                                builder.append("\"\n");
                            });
                    });
            });
    }

    @Override
    public Map<String, Map<String, Map<String, Material>>> def() {
        final var override = overriddenDef();
        if (override != null) {
            return override;
        } else {
            return Arrays.stream(annotation.def()).collect(
                Collectors.toMap(ConfigMaterialMapMapMapEntry::key, e1 ->
                    Arrays.stream(e1.value()).collect(
                        Collectors.toMap(ConfigMaterialMapMapEntry::key, e2 ->
                            Arrays.stream(e2.value()).collect(
                                Collectors.toMap(ConfigMaterialMapEntry::key, e3 -> e3.value())
                            )
                        )
                    )
                )
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
        appendMapDefinition(builder, indent, "# ", def());

        // Definition
        builder.append(indent);
        builder.append(basename());
        builder.append(":\n");
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendMapDefinition(builder, indent, "", def);
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isConfigurationSection(yamlPath())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected group");
        }

        for (var key1 : yaml.getConfigurationSection(yamlPath()).getKeys(false)) {
            final var key1Path = yamlPath() + "." + key1;
            if (!yaml.isConfigurationSection(key1Path)) {
                throw new YamlLoadException("Invalid type for yaml path '" + key1Path + "', expected group");
            }

            for (var key2 : yaml.getConfigurationSection(key1Path).getKeys(false)) {
                final var key2Path = key1Path + "." + key2;
                if (!yaml.isConfigurationSection(key2Path)) {
                    throw new YamlLoadException("Invalid type for yaml path '" + key2Path + "', expected group");
                }

                for (var key3 : yaml.getConfigurationSection(key2Path).getKeys(false)) {
                    final var key3Path = key2Path + "." + key3;
                    if (!yaml.isString(key3Path)) {
                        throw new YamlLoadException("Invalid type for yaml path '" + key3Path + "', expected string");
                    }

                    final var str = yaml.getString(key3Path);
                    final var split = str.split(":");
                    if (split.length != 2) {
                        throw new YamlLoadException(
                            "Invalid material entry in list '" +
                            key3Path +
                            "': '" +
                            str +
                            "' is not a valid namespaced key"
                        );
                    }

                    final var mat = materialFrom(namespacedKey(split[0], split[1]));
                    if (mat == null) {
                        throw new YamlLoadException(
                            "Invalid material entry in list '" + key3Path + "': '" + str + "' does not exist"
                        );
                    }
                }
            }
        }
    }

    public Map<String, Map<String, Map<String, Material>>> loadFromYaml(YamlConfiguration yaml) {
        final var map1 = new HashMap<String, Map<String, Map<String, Material>>>();
        for (final var key1 : yaml.getConfigurationSection(yamlPath()).getKeys(false)) {
            final var key1Path = yamlPath() + "." + key1;
            final var map2 = new HashMap<String, Map<String, Material>>();
            map1.put(key1, map2);
            for (final var key2 : yaml.getConfigurationSection(key1Path).getKeys(false)) {
                final var key2Path = key1Path + "." + key2;
                final var map3 = new HashMap<String, Material>();
                map2.put(key2, map3);
                for (final var key3 : yaml.getConfigurationSection(key2Path).getKeys(false)) {
                    final var key3Path = key2Path + "." + key3;
                    final var split = yaml.getString(key3Path).split(":");
                    map3.put(key3, materialFrom(namespacedKey(split[0], split[1])));
                }
            }
        }
        return map1;
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, loadFromYaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
