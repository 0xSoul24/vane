package org.oddlama.vane.core.lang;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

public abstract class LangField<T> {

    public static final String PREFIX = "lang";

    private Module<?> module;
    protected Object owner;
    protected Field field;
    protected String name;
    private final String namespace;
    private final String key;

    public LangField(Module<?> module, Object owner, Field field, Function<String, String> mapName) {
        this.module = module;
        this.owner = owner;
        this.field = field;

        if (!field.getName().startsWith(PREFIX)) throw new RuntimeException(
            new YamlLoadException.Lang("field must start with " + PREFIX, this)
        );
        this.name = mapName.apply(field.getName().substring(PREFIX.length()));
        this.namespace = module.namespace();
        this.key = namespace + "." + yamlPath();

        field.setAccessible(true);
    }

    public String getName() {
        return name;
    }

    public String yamlPath() {
        return name;
    }

    protected void checkYamlPath(YamlConfiguration yaml) throws YamlLoadException {
        if (!yaml.contains(name, true)) {
            throw new YamlLoadException.Lang("yaml is missing entry with path '" + name + "'", this);
        }
    }

    public Module<?> module() {
        return module;
    }

    public String namespace() {
        return namespace;
    }

    public String key() {
        return key;
    }

    public abstract void checkLoadable(YamlConfiguration yaml) throws YamlLoadException;

    public abstract void load(final String namespace, final YamlConfiguration yaml);

    public abstract void addTranslations(
        final ResourcePackGenerator pack,
        final YamlConfiguration yaml,
        String langCode
    ) throws YamlLoadException;

    @SuppressWarnings("unchecked")
    public T get() {
        try {
            return (T) field.get(owner);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }

    @Override
    public String toString() {
        return (field.getDeclaringClass().getTypeName() + "::" + field.getName());
    }
}
