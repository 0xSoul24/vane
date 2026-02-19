package org.oddlama.vane.core.lang;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.lang.LangMessageArray;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

public class LangMessageArrayField extends LangField<TranslatedMessageArray> {

    public LangMessageArray annotation;

    public LangMessageArrayField(
        Module<?> module,
        Object owner,
        Field field,
        Function<String, String> mapName,
        LangMessageArray annotation
    ) {
        super(module, owner, field, mapName);
        this.annotation = annotation;
    }

    @Override
    public void checkLoadable(final YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isList(yamlPath())) {
            throw new YamlLoadException.Lang("Invalid type for yaml path '" + yamlPath() + "', expected list", this);
        }

        for (final var obj : yaml.getList(yamlPath())) {
            if (!(obj instanceof String)) {
                throw new YamlLoadException.Lang(
                    "Invalid type for yaml path '" + yamlPath() + "', expected string",
                    this
                );
            }
        }
    }

    private List<String> fromYaml(final YamlConfiguration yaml) {
        final var list = new ArrayList<String>();
        for (final var obj : yaml.getList(yamlPath())) {
            list.add((String) obj);
        }
        return list;
    }

    @Override
    public void load(final String namespace, final YamlConfiguration yaml) {
        try {
            field.set(owner, new TranslatedMessageArray(module(), key(), fromYaml(yaml)));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }

    @Override
    public void addTranslations(final ResourcePackGenerator pack, final YamlConfiguration yaml, String langCode)
        throws YamlLoadException {
        checkLoadable(yaml);
        final var list = fromYaml(yaml);
        final var loadedSize = get().size();
        if (list.size() != loadedSize) {
            throw new YamlLoadException.Lang(
                "All translation lists for message arrays must have the exact same size. The loaded language file has " +
                loadedSize +
                " entries, while the currently processed file has " +
                list.size(),
                this
            );
        }
        for (int i = 0; i < list.size(); ++i) {
            pack.translations(namespace(), langCode).put(key() + "." + i, list.get(i));
        }
    }
}
