package org.oddlama.vane.core.lang;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

public class LangMessageField extends LangField<TranslatedMessage> {

    public LangMessage annotation;

    public LangMessageField(
        Module<?> module,
        Object owner,
        Field field,
        Function<String, String> mapName,
        LangMessage annotation
    ) {
        super(module, owner, field, mapName);
        this.annotation = annotation;
    }

    @Override
    public void checkLoadable(final YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isString(yamlPath())) {
            throw new YamlLoadException.Lang("Invalid type for yaml path '" + yamlPath() + "', expected string", this);
        }
    }

    private String fromYaml(final YamlConfiguration yaml) {
        return yaml.getString(yamlPath());
    }

    @Override
    public void load(final String namespace, final YamlConfiguration yaml) {
        try {
            field.set(owner, new TranslatedMessage(module(), key(), fromYaml(yaml)));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }

    @Override
    public void addTranslations(final ResourcePackGenerator pack, final YamlConfiguration yaml, String langCode)
        throws YamlLoadException {
        checkLoadable(yaml);
        pack.translations(namespace(), langCode).put(key(), fromYaml(yaml));
    }
}
