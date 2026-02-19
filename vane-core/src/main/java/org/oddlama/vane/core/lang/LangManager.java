package org.oddlama.vane.core.lang;

import static org.reflections.ReflectionUtils.getAllFields;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.lang.LangMessageArray;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

public class LangManager {

    Module<?> module;
    private List<LangField<?>> langFields = new ArrayList<>();
    LangVersionField fieldVersion;

    public LangManager(Module<?> module) {
        this.module = module;
        compile(module, s -> s);
    }

    public long expectedVersion() {
        return module.annotation.langVersion();
    }

    private boolean hasLangAnnotation(Field field) {
        for (var a : field.getAnnotations()) {
            if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.lang.Lang")) {
                return true;
            }
        }
        return false;
    }

    private void assertFieldPrefix(Field field) {
        if (!field.getName().startsWith("lang")) {
            throw new RuntimeException("Language fields must be prefixed lang. This is a bug.");
        }
    }

    private LangField<?> compileField(Object owner, Field field, Function<String, String> mapName) {
        assertFieldPrefix(field);

        // Get the annotation
        Annotation annotation = null;
        for (var a : field.getAnnotations()) {
            if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.lang.Lang")) {
                if (annotation == null) {
                    annotation = a;
                } else {
                    throw new RuntimeException("Language fields must have exactly one @Lang annotation.");
                }
            }
        }
        assert annotation != null;
        final var atype = annotation.annotationType();

        // Return a correct wrapper object
        if (atype.equals(LangMessage.class)) {
            return new LangMessageField(module, owner, field, mapName, (LangMessage) annotation);
        } else if (atype.equals(LangMessageArray.class)) {
            return new LangMessageArrayField(module, owner, field, mapName, (LangMessageArray) annotation);
        } else if (atype.equals(LangVersion.class)) {
            if (owner != module) {
                throw new RuntimeException("@LangVersion can only be used inside the main module. This is a bug.");
            }
            if (fieldVersion != null) {
                throw new RuntimeException(
                    "There must be exactly one @LangVersion field! (found multiple). This is a bug."
                );
            }
            return fieldVersion = new LangVersionField(module, owner, field, mapName, (LangVersion) annotation);
        } else {
            throw new RuntimeException("Missing LangField handler for @" + atype.getName() + ". This is a bug.");
        }
    }

    private boolean verifyVersion(File file, long version) {
        if (version != expectedVersion()) {
            module.log.severe(file.getName() + ": expected version " + expectedVersion() + ", but got " + version);

            if (version == 0) {
                module.log.severe("Something went wrong while generating or loading the configuration.");
                module.log.severe("If you are sure your configuration is correct and this isn't a file");
                module.log.severe(
                    "system permission issue, please report this to https://github.com/oddlama/vane/issues"
                );
            } else if (version < expectedVersion()) {
                module.log.severe("This language file is for an older version of " + module.getName() + ".");
                module.log.severe("Please update your file or use an officially supported language file.");
            } else {
                module.log.severe("This language file is for a future version of " + module.getName() + ".");
                module.log.severe("Please use the correct file for this version, or use an officially");
                module.log.severe("supported language file.");
            }

            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public void compile(Object owner, Function<String, String> mapName) {
        // Compile all annotated fields
        langFields.addAll(
            getAllFields(owner.getClass())
                .stream()
                .filter(this::hasLangAnnotation)
                .map(f -> compileField(owner, f, mapName))
                .toList()
        );

        if (owner == module && fieldVersion == null) {
            throw new RuntimeException("There must be exactly one @LangVersion field! (found none). This is a bug.");
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getField(String name) {
        var field = langFields
            .stream()
            .filter(f -> f.getName().equals(name))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Missing lang field lang" + name));

        try {
            return (T) field;
        } catch (ClassCastException e) {
            throw new RuntimeException("Invalid lang field type for lang" + name, e);
        }
    }

    public boolean reload(File file) {
        // Load file
        var yaml = YamlConfiguration.loadConfiguration(file);

        // Check version
        final var version = yaml.getLong("Version", -1);
        if (!verifyVersion(file, version)) {
            return false;
        }

        try {
            // Check languration for errors
            for (var f : langFields) {
                f.checkLoadable(yaml);
            }

            for (var f : langFields) {
                f.load(module.namespace(), yaml);
            }
        } catch (YamlLoadException e) {
            module.log.log(Level.SEVERE, "error while loading '" + file.getAbsolutePath() + "'", e);
            return false;
        }
        return true;
    }

    public void generateResourcePack(final ResourcePackGenerator pack, YamlConfiguration yaml, File langFile) {
        var langCode = yaml.getString("ResourcePackLangCode");
        if (langCode == null) {
            throw new RuntimeException("Missing yaml key: ResourcePackLangCode");
        }
        var errors = new LinkedList<YamlLoadException.Lang>();
        for (var f : langFields) {
            try {
                f.addTranslations(pack, yaml, langCode);
            } catch (YamlLoadException.Lang e) {
                errors.add(e);
            } catch (YamlLoadException e) {
                module.log.log(Level.SEVERE, "Unexpected YAMLLoadException: ", e);
            }
        }
        if (errors.size() > 0) {
            final String erroredLangNodes = errors
                .stream()
                .map(Throwable::getMessage)
                .collect(Collectors.joining("\n\t\t"));
            module.log.log(
                Level.SEVERE,
                "The following errors were identified while adding translations from \n\t" +
                langFile.getAbsolutePath() +
                " \n\t\t" +
                erroredLangNodes
            );
        }
    }
}
