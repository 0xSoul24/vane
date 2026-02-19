package org.oddlama.vane.core.lang;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

public class LangVersionField extends LangField<Long> {

    public LangVersion annotation;

    public LangVersionField(
        Module<?> module,
        Object owner,
        Field field,
        Function<String, String> mapName,
        LangVersion annotation
    ) {
        super(module, owner, field, mapName);
        this.annotation = annotation;
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!(yaml.get(yamlPath()) instanceof Number)) {
            throw new YamlLoadException.Lang("Invalid type for yaml path '" + yamlPath() + "', expected long", this);
        }

        var val = yaml.getLong(yamlPath());
        if (val < 1) {
            throw new YamlLoadException.Lang(
                "Entry '" + yamlPath() + "' has an invalid value: Value must be >= 1",
                this
            );
        }
    }

    @Override
    public void load(final String namespace, final YamlConfiguration yaml) {
        try {
            field.setLong(owner, yaml.getLong(yamlPath()));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }

    @Override
    public void addTranslations(final ResourcePackGenerator pack, final YamlConfiguration yaml, String langCode)
        throws YamlLoadException {}
}
