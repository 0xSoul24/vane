package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.StorageUtil.namespacedKey;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.material.ExtendedMaterial;

public class ConfigExtendedMaterialField extends ConfigField<ExtendedMaterial> {

    public ConfigExtendedMaterial annotation;

    public ConfigExtendedMaterialField(
        Object owner,
        Field field,
        Function<String, String> mapName,
        ConfigExtendedMaterial annotation
    ) {
        super(owner, field, mapName, "extended material", annotation.desc());
        this.annotation = annotation;
    }

    @Override
    public ExtendedMaterial def() {
        final var override = overriddenDef();
        if (override != null) {
            return override;
        } else {
            final var split = annotation.def().split(":");
            if (split.length != 2) {
                throw new RuntimeException(
                    "Invalid default extended material entry for '" +
                    yamlPath() +
                    "': '" +
                    annotation.def() +
                    "' is not a valid namespaced key"
                );
            }
            return ExtendedMaterial.from(namespacedKey(split[0], split[1]));
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
            "\"" + escapeYaml(def().key().getNamespace()) + ":" + escapeYaml(def().key().getKey()) + "\""
        );
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendFieldDefinition(
            builder,
            indent,
            "\"" + escapeYaml(def.key().getNamespace()) + ":" + escapeYaml(def.key().getKey()) + "\""
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
                "Invalid extended material entry in list '" +
                yamlPath() +
                "': '" +
                str +
                "' is not a valid namespaced key"
            );
        }

        final var mat = ExtendedMaterial.from(namespacedKey(split[0], split[1]));
        if (mat == null) {
            throw new YamlLoadException(
                "Invalid extended material entry in list '" + yamlPath() + "': '" + str + "' does not exist"
            );
        }
    }

    public ExtendedMaterial loadFromYaml(YamlConfiguration yaml) {
        final var split = yaml.getString(yamlPath()).split(":");
        return ExtendedMaterial.from(namespacedKey(split[0], split[1]));
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, loadFromYaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
