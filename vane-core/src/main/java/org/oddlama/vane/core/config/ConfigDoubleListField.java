package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigDoubleListField extends ConfigField<List<Double>> {

    public ConfigDoubleList annotation;

    public ConfigDoubleListField(
        Object owner,
        Field field,
        Function<String, String> mapName,
        ConfigDoubleList annotation
    ) {
        super(owner, field, mapName, "double list", annotation.desc());
        this.annotation = annotation;
    }

    private void appendDoubleListDefinition(StringBuilder builder, String indent, String prefix, List<Double> def) {
        appendListDefinition(builder, indent, prefix, def, (b, d) -> b.append(d));
    }

    @Override
    public List<Double> def() {
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
        appendValueRange(builder, indent, annotation.min(), annotation.max(), Double.NaN, Double.NaN);

        // Default
        builder.append(indent);
        builder.append("# Default:\n");
        appendDoubleListDefinition(builder, indent, "# ", def());

        // Definition
        builder.append(indent);
        builder.append(basename());
        builder.append(":\n");
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendDoubleListDefinition(builder, indent, "", def);
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isList(yamlPath())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected list");
        }

        for (var obj : yaml.getList(yamlPath())) {
            if (!(obj instanceof Number)) {
                throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected double");
            }

            var val = yaml.getDouble(yamlPath());
            if (!Double.isNaN(annotation.min()) && val < annotation.min()) {
                throw new YamlLoadException(
                    "Configuration '" + yamlPath() + "' has an invalid value: Value must be >= " + annotation.min()
                );
            }
            if (!Double.isNaN(annotation.max()) && val > annotation.max()) {
                throw new YamlLoadException(
                    "Configuration '" + yamlPath() + "' has an invalid value: Value must be <= " + annotation.max()
                );
            }
        }
    }

    public List<Double> loadFromYaml(YamlConfiguration yaml) {
        final var list = new ArrayList<Double>();
        for (var obj : yaml.getList(yamlPath())) {
            list.add(((Number) obj).doubleValue());
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