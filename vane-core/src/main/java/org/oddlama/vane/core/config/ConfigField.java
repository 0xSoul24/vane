package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.text.WordUtils;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.functional.Consumer2;

public abstract class ConfigField<T> implements Comparable<ConfigField<?>> {

    protected Object owner;
    protected Field field;
    protected String path;
    protected String typeName;
    protected int sortPriority = 0;

    private String[] yamlPathComponents;
    private String yamlGroupPath;
    private String basename;
    private Supplier<String> description;

    public ConfigField(
        Object owner,
        Field field,
        Function<String, String> mapName,
        String typeName,
        String description
    ) {
        this.owner = owner;
        this.field = field;
        this.path = mapName.apply(field.getName().substring("config".length()));
        this.yamlPathComponents = path.split("\\.");

        var lastDot = path.lastIndexOf(".");
        this.yamlGroupPath = lastDot == -1 ? "" : path.substring(0, lastDot);

        this.basename = yamlPathComponents[yamlPathComponents.length - 1];
        this.typeName = typeName;

        // lang, enabled, metrics_enabled should be at the top
        switch (this.path) {
            case "Lang" -> this.sortPriority = -10;
            case "Enabled" -> this.sortPriority = -9;
            case "MetricsEnabled" -> this.sortPriority = -8;
        }

        field.setAccessible(true);

        // Dynamic description
        this.description = () -> {
            try {
                return (String) owner.getClass().getMethod(field.getName() + "Desc").invoke(owner);
            } catch (NoSuchMethodException e) {
                // Ignore, field wasn't overridden
                return description;
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(
                    "Could not call " +
                    owner.getClass().getName() +
                    "." +
                    field.getName() +
                    "Desc() to override description value",
                    e
                );
            }
        };
    }

    @SuppressWarnings("unchecked")
    protected T overriddenDef() {
        try {
            return (T) owner.getClass().getMethod(field.getName() + "Def").invoke(owner);
        } catch (NoSuchMethodException e) {
            // Ignore, field wasn't overridden
            return null;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(
                "Could not call " +
                owner.getClass().getName() +
                "." +
                field.getName() +
                "Def() to override default value",
                e
            );
        }
    }

    protected Boolean overriddenMetrics() {
        try {
            return (Boolean) owner.getClass().getMethod(field.getName() + "Metrics").invoke(owner);
        } catch (NoSuchMethodException e) {
            // Ignore, field wasn't overridden
            return null;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(
                "Could not call " +
                owner.getClass().getName() +
                "." +
                field.getName() +
                "Metrics() to override metrics status",
                e
            );
        }
    }

    protected String escapeYaml(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public String getYamlGroupPath() {
        return path;
    }

    public String yamlPath() {
        return path;
    }

    public String yamlGroupPath() {
        return yamlGroupPath;
    }

    public String basename() {
        return basename;
    }

    private String modifyYamlPathForSorting(String path) {
        // "enable" fields should always be at the top, and therefore
        // get treated without the suffix.
        if (path.endsWith(".enabled")) {
            return path.substring(0, path.lastIndexOf(".enabled"));
        }
        return path;
    }

    @Override
    public int compareTo(ConfigField<?> other) {
        if (sortPriority != other.sortPriority) {
            return sortPriority - other.sortPriority;
        } else {
            for (int i = 0; i < Math.min(yamlPathComponents.length, other.yamlPathComponents.length) - 1; ++i) {
                var c = yamlPathComponents[i].compareTo(other.yamlPathComponents[i]);
                if (c != 0) {
                    return c;
                }
            }
            return modifyYamlPathForSorting(yamlPath()).compareTo(modifyYamlPathForSorting(other.yamlPath()));
        }
    }

    protected void appendDescription(StringBuilder builder, String indent) {
        final var descriptionWrapped =
            indent +
            "# " +
            WordUtils.wrap(description.get(), Math.max(60, 80 - indent.length()), "\n" + indent + "# ", false);
        builder.append(descriptionWrapped);
        builder.append("\n");
    }

    protected <U> void appendListDefinition(
        StringBuilder builder,
        String indent,
        String prefix,
        Collection<U> list,
        Consumer2<StringBuilder, U> append
    ) {
        list
            .stream()
            .forEach(i -> {
                builder.append(indent);
                builder.append(prefix);
                builder.append("  - ");
                append.apply(builder, i);
                builder.append("\n");
            });
    }

    protected <U> void appendValueRange(
        StringBuilder builder,
        String indent,
        U min,
        U max,
        U invalidMin,
        U invalidMax
    ) {
        builder.append(indent);
        builder.append("# Valid values: ");
        if (!min.equals(invalidMin)) {
            if (!max.equals(invalidMax)) {
                builder.append("[");
                builder.append(min);
                builder.append(",");
                builder.append(max);
                builder.append("]");
            } else {
                builder.append("[");
                builder.append(min);
                builder.append(",)");
            }
        } else {
            if (!max.equals(invalidMax)) {
                builder.append("(,");
                builder.append(max);
                builder.append("]");
            } else {
                builder.append("Any " + typeName);
            }
        }
        builder.append("\n");
    }

    protected void appendDefaultValue(StringBuilder builder, String indent, Object def) {
        builder.append(indent);
        builder.append("# Default: ");
        builder.append(def);
        builder.append("\n");
    }

    protected void appendFieldDefinition(StringBuilder builder, String indent, Object def) {
        builder.append(indent);
        builder.append(basename);
        builder.append(": ");
        builder.append(def);
        builder.append("\n");
    }

    protected void checkYamlPath(YamlConfiguration yaml) throws YamlLoadException {
        if (!yaml.contains(path, true)) {
            throw new YamlLoadException("yaml is missing entry with path '" + path + "'");
        }
    }

    public abstract T def();

    // Disabled by default, fields must explicitly support this!
    public boolean metrics() {
        return false;
    }

    public abstract void generateYaml(
        StringBuilder builder,
        String indent,
        YamlConfiguration existingCompatibleConfig
    );

    public abstract void checkLoadable(YamlConfiguration yaml) throws YamlLoadException;

    public abstract void load(YamlConfiguration yaml);

    @SuppressWarnings("unchecked")
    public T get() {
        try {
            return (T) field.get(owner);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }

    public void registerMetrics(Metrics metrics) {
        if (!metrics()) return;
        metrics.addCustomChart(new SimplePie(yamlPath(), () -> get().toString()));
    }

    public String[] components() {
        return yamlPathComponents;
    }

    public int groupCount() {
        return yamlPathComponents.length - 1;
    }

    public static boolean sameGroup(ConfigField<?> a, ConfigField<?> b) {
        if (a.yamlPathComponents.length != b.yamlPathComponents.length) {
            return false;
        }
        for (int i = 0; i < a.yamlPathComponents.length - 1; ++i) {
            if (!a.yamlPathComponents[i].equals(b.yamlPathComponents[i])) {
                return false;
            }
        }
        return true;
    }

    public static int commonGroupCount(ConfigField<?> a, ConfigField<?> b) {
        int i;
        for (i = 0; i < Math.min(a.yamlPathComponents.length, b.yamlPathComponents.length) - 1; ++i) {
            if (!a.yamlPathComponents[i].equals(b.yamlPathComponents[i])) {
                return i;
            }
        }
        return i;
    }
}