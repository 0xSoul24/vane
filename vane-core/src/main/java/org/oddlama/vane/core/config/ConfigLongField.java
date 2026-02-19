package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigLongField extends ConfigField<Long> {

    public ConfigLong annotation;

    public ConfigLongField(Object owner, Field field, Function<String, String> mapName, ConfigLong annotation) {
        super(owner, field, mapName, "long", annotation.desc());
        this.annotation = annotation;
    }

    @Override
    public Long def() {
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
        appendValueRange(builder, indent, annotation.min(), annotation.max(), Long.MIN_VALUE, Long.MAX_VALUE);
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
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected long");
        }

        var val = yaml.getLong(yamlPath());
        if (annotation.min() != Long.MIN_VALUE && val < annotation.min()) {
            throw new YamlLoadException(
                "Configuration '" + yamlPath() + "' has an invalid value: Value must be >= " + annotation.min()
            );
        }
        if (annotation.max() != Long.MAX_VALUE && val > annotation.max()) {
            throw new YamlLoadException(
                "Configuration '" + yamlPath() + "' has an invalid value: Value must be <= " + annotation.max()
            );
        }
    }

    public long loadFromYaml(YamlConfiguration yaml) {
        return yaml.getLong(yamlPath());
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.setLong(owner, loadFromYaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
