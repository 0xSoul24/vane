package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigStringList;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigStringListField extends ConfigField<List<String>> {

    public ConfigStringList annotation;

    public ConfigStringListField(
        Object owner,
        Field field,
        Function<String, String> mapName,
        ConfigStringList annotation
    ) {
        super(owner, field, mapName, "list of strings", annotation.desc());
        this.annotation = annotation;
    }

    private void appendStringListDefinition(StringBuilder builder, String indent, String prefix, List<String> def) {
        appendListDefinition(builder, indent, prefix, def, (b, s) -> {
            b.append("\"");
            b.append(escapeYaml(s));
            b.append("\"");
        });
    }

    @Override
    public List<String> def() {
        final var override = overriddenDef();
        if (override != null) {
            return override;
        } else {
            return Arrays.asList(annotation.def());
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

        // Default
        builder.append(indent);
        builder.append("# Default:\n");
        appendStringListDefinition(builder, indent, "# ", def());

        // Definition
        builder.append(indent);
        builder.append(basename());
        builder.append(":\n");
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendStringListDefinition(builder, indent, "", def);
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isList(yamlPath())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected list");
        }

        for (final var obj : yaml.getList(yamlPath())) {
            if (!(obj instanceof String)) {
                throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected string");
            }
        }
    }

    public List<String> loadFromYaml(YamlConfiguration yaml) {
        final var list = new ArrayList<String>();
        for (var obj : yaml.getList(yamlPath())) {
            list.add((String) obj);
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
