package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigStringField extends ConfigField<String> {

    public ConfigString annotation;

    public ConfigStringField(Object owner, Field field, Function<String, String> mapName, ConfigString annotation) {
        super(owner, field, mapName, "string", annotation.desc());
        this.annotation = annotation;
    }

    @Override
    public String def() {
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
        appendDefaultValue(builder, indent, "\"" + escapeYaml(def()) + "\"");
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendFieldDefinition(builder, indent, "\"" + escapeYaml(def) + "\"");
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isString(yamlPath())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected string");
        }
    }

    public String loadFromYaml(YamlConfiguration yaml) {
        return yaml.getString(yamlPath());
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, loadFromYaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
