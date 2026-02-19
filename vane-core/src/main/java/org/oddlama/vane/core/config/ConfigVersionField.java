package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Module;

public class ConfigVersionField extends ConfigField<Long> {

    public ConfigVersion annotation;

    public ConfigVersionField(Object owner, Field field, Function<String, String> mapName, ConfigVersion annotation) {
        super(
            owner,
            field,
            mapName,
            "version id",
            "DO NOT CHANGE! The version of this config file. Used to determine if the config needs to be updated."
        );
        this.annotation = annotation;

        // Version field should be at the bottom
        this.sortPriority = 100;
    }

    @Override
    public Long def() {
        return null;
    }

    @Override
    public boolean metrics() {
        return true;
    }

    @Override
    public void generateYaml(StringBuilder builder, String indent, YamlConfiguration existingCompatibleConfig) {
        appendDescription(builder, indent);
        appendFieldDefinition(builder, indent, ((Module<?>) owner).annotation.configVersion());
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!(yaml.get(yamlPath()) instanceof Number)) {
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected long");
        }

        var val = yaml.getLong(yamlPath());
        if (val < 1) {
            throw new YamlLoadException("Configuration '" + yamlPath() + "' has an invalid value: Value must be >= 1");
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
