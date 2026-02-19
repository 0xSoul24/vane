package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.MaterialUtil.materialFrom;
import static org.oddlama.vane.util.StorageUtil.namespacedKey;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigMaterialSetField extends ConfigField<Set<Material>> {

    public ConfigMaterialSet annotation;

    public ConfigMaterialSetField(
        Object owner,
        Field field,
        Function<String, String> mapName,
        ConfigMaterialSet annotation
    ) {
        super(owner, field, mapName, "set of materials", annotation.desc());
        this.annotation = annotation;
    }

    private void appendMaterialSetDefinition(
        StringBuilder builder,
        String indent,
        String prefix,
        Set<Material> def
    ) {
        appendListDefinition(builder, indent, prefix, def, (b, m) -> {
            b.append("\"");
            b.append(escapeYaml(m.getKey().getNamespace()));
            b.append(":");
            b.append(escapeYaml(m.getKey().getKey()));
            b.append("\"");
        });
    }

    @Override
    public Set<Material> def() {
        final var override = overriddenDef();
        if (override != null) {
            return override;
        } else {
            return new HashSet<>(Arrays.asList(annotation.def()));
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
    public void registerMetrics(Metrics metrics) {
        if (!this.metrics()) return;
        metrics.addCustomChart(
            new AdvancedPie(yamlPath(), () -> {
                final var values = new HashMap<String, Integer>();
                for (final var v : get()) {
                    values.put(v.getKey().toString(), 1);
                }
                return values;
            })
        );
    }

    @Override
    public void generateYaml(StringBuilder builder, String indent, YamlConfiguration existingCompatibleConfig) {
        appendDescription(builder, indent);

        // Default
        builder.append(indent);
        builder.append("# Default:\n");
        appendMaterialSetDefinition(builder, indent, "# ", def());

        // Definition
        builder.append(indent);
        builder.append(basename());
        builder.append(":\n");
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendMaterialSetDefinition(builder, indent, "", def);
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isList(yamlPath())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected list");
        }

        for (var obj : yaml.getList(yamlPath())) {
            if (!(obj instanceof String)) {
                throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected string");
            }

            final var str = (String) obj;
            final var split = str.split(":");
            if (split.length != 2) {
                throw new YamlLoadException(
                    "Invalid material entry in list '" + yamlPath() + "': '" + str + "' is not a valid namespaced key"
                );
            }

            final var mat = materialFrom(namespacedKey(split[0], split[1]));
            if (mat == null) {
                throw new YamlLoadException(
                    "Invalid material entry in list '" + yamlPath() + "': '" + str + "' does not exist"
                );
            }
        }
    }

    public Set<Material> loadFromYaml(YamlConfiguration yaml) {
        final var set = new HashSet<Material>();
        for (var obj : yaml.getList(yamlPath())) {
            final var split = ((String) obj).split(":");
            set.add(materialFrom(namespacedKey(split[0], split[1])));
        }
        return set;
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, loadFromYaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
