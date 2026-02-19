package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigDict;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigDictField extends ConfigField<ConfigDictSerializable> {

    private class EmptyDict implements ConfigDictSerializable {

        @Override
        public Map<String, Object> toDict() {
            return new HashMap<>();
        }

        @Override
        public void fromDict(final Map<String, Object> dict) {
            // no-op
        }
    }

    public ConfigDict annotation;

    public ConfigDictField(
        final Object owner,
        final Field field,
        final Function<String, String> mapName,
        final ConfigDict annotation
    ) {
        super(owner, field, mapName, "dict", annotation.desc());
        this.annotation = annotation;
    }

    @Override
    public ConfigDictSerializable def() {
        final var override = overriddenDef();
        if (override != null) {
            return override;
        } else {
            return new EmptyDict();
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

    @SuppressWarnings("unchecked")
    private void appendList(
        final StringBuilder builder,
        final String indent,
        final String listKey,
        final List<Object> list
    ) {
        builder.append(indent);
        builder.append(listKey);
        if (list.isEmpty()) {
            builder.append(": []\n");
        } else {
            builder.append(":\n");
            list.forEach(entry -> {
                if (entry instanceof String) {
                    builder.append(indent);
                    builder.append("  - ");
                    builder.append("\"" + escapeYaml(entry.toString()) + "\"");
                    builder.append("\n");
                } else if (
                    entry instanceof Integer ||
                    entry instanceof Long ||
                    entry instanceof Float ||
                    entry instanceof Double ||
                    entry instanceof Boolean
                ) {
                    builder.append(indent);
                    builder.append("  - ");
                    builder.append(entry);
                    builder.append("\n");
                } else if (entry instanceof Map<?, ?>) {
                    appendDict(builder, indent + "  ", null, (Map<String, Object>) entry, true);
                } else {
                    throw new RuntimeException(
                        "Invalid value '" +
                        entry +
                        "' of type " +
                        entry.getClass() +
                        " in mapping of ConfigDictSerializable"
                    );
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void appendDict(
        final StringBuilder builder,
        final String indent,
        final String dictKey,
        final Map<String, Object> dict,
        final boolean isListEntry
    ) {
        builder.append(indent);
        if (isListEntry) {
            builder.append("-");
        } else {
            builder.append(dictKey);
            builder.append(":");
        }
        if (dict.isEmpty()) {
            builder.append(" {}\n");
        } else {
            builder.append("\n");
            dict
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (entry.getValue() instanceof String) {
                        builder.append(indent + "  ");
                        builder.append(entry.getKey());
                        builder.append(": ");
                        builder.append("\"" + escapeYaml(entry.getValue().toString()) + "\"");
                        builder.append("\n");
                    } else if (
                        entry.getValue() instanceof Integer ||
                        entry.getValue() instanceof Long ||
                        entry.getValue() instanceof Float ||
                        entry.getValue() instanceof Double ||
                        entry.getValue() instanceof Boolean
                    ) {
                        builder.append(indent + "  ");
                        builder.append(entry.getKey());
                        builder.append(": ");
                        builder.append(entry.getValue().toString());
                        builder.append("\n");
                    } else if (entry.getValue() instanceof Map<?, ?>) {
                        appendDict(
                            builder,
                            indent + "  ",
                            entry.getKey(),
                            (Map<String, Object>) entry.getValue(),
                            false
                        );
                    } else if (entry.getValue() instanceof List<?>) {
                        appendList(builder, indent + "  ", entry.getKey(), (List<Object>) entry.getValue());
                    } else {
                        throw new RuntimeException(
                            "Invalid value '" +
                            entry.getValue() +
                            "' of type " +
                            entry.getValue().getClass() +
                            " in mapping of ConfigDictSerializable"
                        );
                    }
                });
        }
    }

    private void appendDict(
        final StringBuilder builder,
        final String indent,
        final boolean defaultDefinition,
        final ConfigDictSerializable ser
    ) {
        if (defaultDefinition) {
            appendDict(builder, indent + "# ", "Default", ser.toDict(), false);
        } else {
            appendDict(builder, indent, basename(), ser.toDict(), false);
        }
    }

    @Override
    public void generateYaml(
        final StringBuilder builder,
        final String indent,
        final YamlConfiguration existingCompatibleConfig
    ) {
        appendDescription(builder, indent);
        appendDict(builder, indent, true, def());
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendDict(builder, indent, false, def);
    }

    @Override
    public void checkLoadable(final YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isConfigurationSection(yamlPath())) {
            throw new YamlLoadException(
                "Invalid type for yaml path '" + yamlPath() + "', expected configuration section"
            );
        }
    }

    public ArrayList<Object> loadListFromYaml(final List<?> rawList) {
        final var list = new ArrayList<Object>();
        for (var e : rawList) {
            if (e instanceof ConfigurationSection section) {
                list.add(loadDictFromYaml(section));
            } else {
                list.add(e);
            }
        }
        return list;
    }

    public HashMap<String, Object> loadDictFromYaml(final ConfigurationSection section) {
        final var dict = new HashMap<String, Object>();
        for (var subkey : section.getKeys(false)) {
            if (section.isConfigurationSection(subkey)) {
                dict.put(subkey, loadDictFromYaml(section.getConfigurationSection(subkey)));
            } else if (section.isList(subkey)) {
                dict.put(subkey, loadListFromYaml(section.getList(subkey)));
            } else if (section.isString(subkey)) {
                dict.put(subkey, section.getString(subkey));
            } else if (section.isInt(subkey)) {
                dict.put(subkey, section.getInt(subkey));
            } else if (section.isDouble(subkey)) {
                dict.put(subkey, section.getDouble(subkey));
            } else if (section.isBoolean(subkey)) {
                dict.put(subkey, section.getBoolean(subkey));
            } else if (section.isLong(subkey)) {
                dict.put(subkey, section.getLong(subkey));
            } else {
                throw new IllegalStateException(
                    "Cannot load dict entry '" + yamlPath() + "." + subkey + "': unknown type"
                );
            }
        }
        return dict;
    }

    public ConfigDictSerializable loadFromYaml(final YamlConfiguration yaml) {
        try {
            final var dict = ((ConfigDictSerializable) annotation.cls().getDeclaredConstructor().newInstance());
            dict.fromDict(loadDictFromYaml(yaml.getConfigurationSection(yamlPath())));
            return dict;
        } catch (
            InstantiationException
            | IllegalAccessException
            | IllegalArgumentException
            | InvocationTargetException
            | NoSuchMethodException
            | SecurityException e
        ) {
            throw new RuntimeException("Could not instanciate storage class for ConfigDict: " + annotation.cls(), e);
        }
    }

    public void load(final YamlConfiguration yaml) {
        try {
            field.set(owner, loadFromYaml(yaml));
        } catch (final IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
