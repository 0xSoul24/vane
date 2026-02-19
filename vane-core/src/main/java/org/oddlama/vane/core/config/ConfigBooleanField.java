package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigBooleanField extends ConfigField<Boolean> {

    private ConfigBoolean annotation;

    public ConfigBooleanField(Object owner, Field field, Function<String, String> mapName, ConfigBoolean annotation) {
        super(owner, field, mapName, "boolean", annotation.desc());
        this.annotation = annotation;
    }

    @Override
    public Boolean def() {
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
        appendDefaultValue(builder, indent, def());
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendFieldDefinition(builder, indent, def);
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isBoolean(yamlPath())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected boolean");
        }
    }

    public boolean loadFromYaml(YamlConfiguration yaml) {
        return yaml.getBoolean(yamlPath());
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.setBoolean(owner, loadFromYaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
