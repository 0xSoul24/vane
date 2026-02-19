package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.MaterialUtil.materialFrom;
import static org.oddlama.vane.util.StorageUtil.namespacedKey;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigMaterialField extends ConfigField<Material> {

    public ConfigMaterial annotation;

    public ConfigMaterialField(
        Object owner,
        Field field,
        Function<String, String> mapName,
        ConfigMaterial annotation
    ) {
        super(owner, field, mapName, "material", annotation.desc());
        this.annotation = annotation;
    }

    @Override
    public Material def() {
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
        appendDefaultValue(
            builder,
            indent,
            "\"" + escapeYaml(def().getKey().getNamespace()) + ":" + escapeYaml(def().getKey().getKey()) + "\""
        );
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendFieldDefinition(
            builder,
            indent,
            "\"" + escapeYaml(def.getKey().getNamespace()) + ":" + escapeYaml(def.getKey().getKey()) + "\""
        );
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isString(yamlPath())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected string");
        }

        final var str = yaml.getString(yamlPath());
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

    public Material loadFromYaml(YamlConfiguration yaml) {
        final var split = yaml.getString(yamlPath()).split(":");
        return materialFrom(namespacedKey(split[0], split[1]));
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, loadFromYaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
