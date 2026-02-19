package org.oddlama.vane.core.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.oddlama.vane.core.module.Module;

public class TranslatedMessageArray {

    private Module<?> module;
    private String key;
    private List<String> defaultTranslation;

    public TranslatedMessageArray(final Module<?> module, final String key, final List<String> defaultTranslation) {
        this.module = module;
        this.key = key;
        this.defaultTranslation = defaultTranslation;
    }

    public int size() {
        return defaultTranslation.size();
    }

    public String key() {
        return key;
    }

    public List<String> str(Object... args) {
        try {
            final var argsAsStrings = new Object[args.length];
            for (int i = 0; i < args.length; ++i) {
                if (args[i] instanceof Component) {
                    argsAsStrings[i] = LegacyComponentSerializer.legacySection().serialize((Component) args[i]);
                } else if (args[i] instanceof String) {
                    argsAsStrings[i] = args[i];
                } else {
                    throw new RuntimeException(
                        "Error while formatting message '" +
                        key() +
                        "', invalid argument to str() serializer: " +
                        args[i]
                    );
                }
            }

            final var list = new ArrayList<String>();
            for (final var s : defaultTranslation) {
                list.add(String.format(s, argsAsStrings));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Error while formatting message '" + key() + "'", e);
        }
    }

    public List<Component> format(Object... args) {
        if (!module.core.configClientSideTranslations) {
            return str(args)
                .stream()
                .map(s -> LegacyComponentSerializer.legacySection().deserialize(s))
                .collect(Collectors.toList());
        }

        final var arr = new ArrayList<Component>();
        for (int i = 0; i < defaultTranslation.size(); ++i) {
            final var list = new ArrayList<ComponentLike>();
            for (final var o : args) {
                if (o instanceof ComponentLike) {
                    list.add((ComponentLike) o);
                } else if (o instanceof String) {
                    list.add(LegacyComponentSerializer.legacySection().deserialize((String) o));
                } else {
                    throw new RuntimeException(
                        "Error while formatting message '" + key() + "', got invalid argument " + o
                    );
                }
            }
            arr.add(Component.translatable(key + "." + i, list));
        }
        return arr;
    }
}
