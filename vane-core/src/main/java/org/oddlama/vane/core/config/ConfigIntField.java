package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigIntField extends ConfigField<Integer> {

    public ConfigInt annotation;

    public ConfigIntField(Object owner, Field field, Function<String, String> mapName, ConfigInt annotation) {
        super(owner, field, mapName, "int", annotation.desc());
        this.annotation = annotation;
    }

    @Override
    public Integer def() {
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
        appendValueRange(builder, indent, annotation.min(), annotation.max(), Integer.MIN_VALUE, Integer.MAX_VALUE);
        appendDefaultValue(builder, indent, def());
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendFieldDefinition(builder, indent, def);
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!(yaml.get(yamlPath()) instanceof Number)) {
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected int");
        }

        final var val = yaml.getInt(yamlPath());
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

    public int loadFromYaml(YamlConfiguration yaml) {
        return yaml.getInt(yamlPath());
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.setInt(owner, loadFromYaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
