package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigIntList;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigIntListField extends ConfigField<List<Integer>> {

    public ConfigIntList annotation;

    public ConfigIntListField(Object owner, Field field, Function<String, String> mapName, ConfigIntList annotation) {
        super(owner, field, mapName, "int list", annotation.desc());
        this.annotation = annotation;
    }

    private void appendIntListDefinition(StringBuilder builder, String indent, String prefix, List<Integer> def) {
        appendListDefinition(builder, indent, prefix, def, (b, i) -> b.append(i));
    }

    @Override
    public List<Integer> def() {
        final var override = overriddenDef();
        if (override != null) {
            return override;
        } else {
            return Arrays.asList(ArrayUtils.toObject(annotation.def()));
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
        appendValueRange(builder, indent, annotation.min(), annotation.max(), Integer.MIN_VALUE, Integer.MAX_VALUE);

        // Default
        builder.append(indent);
        builder.append("# Default:\n");
        appendIntListDefinition(builder, indent, "# ", def());

        // Definition
        builder.append(indent);
        builder.append(basename());
        builder.append(":\n");
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendIntListDefinition(builder, indent, "", def);
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isList(yamlPath())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected list");
        }

        for (var obj : yaml.getList(yamlPath())) {
            if (!(obj instanceof Number)) {
                throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected int");
            }

            var val = yaml.getInt(yamlPath());
            if (annotation.min() != Integer.MIN_VALUE && val < annotation.min()) {
                throw new YamlLoadException(
                    "Configuration '" + yamlPath() + "' has an invalid value: Value must be >= " + annotation.min()
                );
            }
            if (annotation.max() != Integer.MAX_VALUE && val > annotation.max()) {
                throw new YamlLoadException(
                    "Configuration '" + yamlPath() + "' has an invalid value: Value must be <= " + annotation.max()
                );
            }
        }
    }

    public List<Integer> loadFromYaml(YamlConfiguration yaml) {
        final var list = new ArrayList<Integer>();
        for (var obj : yaml.getList(yamlPath())) {
            list.add(((Number) obj).intValue());
        }
        return list;
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, loadFromYaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}