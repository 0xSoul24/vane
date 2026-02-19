package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigDoubleField extends ConfigField<Double> {

    public ConfigDouble annotation;

    public ConfigDoubleField(Object owner, Field field, Function<String, String> mapName, ConfigDouble annotation) {
        super(owner, field, mapName, "double", annotation.desc());
        this.annotation = annotation;
    }

    @Override
    public Double def() {
        final var override = overriddenDef();
        if (override != null) {
            return override;
        } else {
            return annotation.def();
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
        appendDefaultValue(builder, indent, def());
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendFieldDefinition(builder, indent, def);
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isDouble(yamlPath())) {
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

    public double loadFromYaml(YamlConfiguration yaml) {
        return yaml.getDouble(yamlPath());
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.setDouble(owner, loadFromYaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
