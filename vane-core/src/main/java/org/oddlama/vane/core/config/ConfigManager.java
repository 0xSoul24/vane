package org.oddlama.vane.core.config;

import static org.reflections.ReflectionUtils.getAllFields;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import org.apache.commons.text.WordUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigDict;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigIntList;
import org.oddlama.vane.annotation.config.ConfigItemStack;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap;
import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.config.ConfigStringList;
import org.oddlama.vane.annotation.config.ConfigStringListMap;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;

public class ConfigManager {

    private List<ConfigField<?>> configFields = new ArrayList<>();
    private Map<String, String> sectionDescriptions = new HashMap<>();
    ConfigVersionField fieldVersion;
    Module<?> module;

    public ConfigManager(Module<?> module) {
        this.module = module;
        compile(module, s -> s);
    }

    public long expectedVersion() {
        return module.annotation.configVersion();
    }

    private boolean hasConfigAnnotation(Field field) {
        for (var a : field.getAnnotations()) {
            if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.config.Config")) {
                return true;
            }
        }
        return false;
    }

    private void assertFieldPrefix(Field field) {
        if (!field.getName().startsWith("config")) {
            throw new RuntimeException("Configuration fields must be prefixed config. This is a bug.");
        }
    }

    private ConfigField<?> compileField(Object owner, Field field, Function<String, String> mapName) {
        assertFieldPrefix(field);

        // Get the annotation
        Annotation annotation = null;
        for (var a : field.getAnnotations()) {
            if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.config.Config")) {
                if (annotation == null) {
                    annotation = a;
                } else {
                    throw new RuntimeException("Configuration fields must have exactly one @Config annotation.");
                }
            }
        }
        assert annotation != null;
        final var atype = annotation.annotationType();

        // Return a correct wrapper object
        if (atype.equals(ConfigBoolean.class)) {
            return new ConfigBooleanField(owner, field, mapName, (ConfigBoolean) annotation);
        } else if (atype.equals(ConfigDict.class)) {
            return new ConfigDictField(owner, field, mapName, (ConfigDict) annotation);
        } else if (atype.equals(ConfigDouble.class)) {
            return new ConfigDoubleField(owner, field, mapName, (ConfigDouble) annotation);
        } else if (atype.equals(ConfigDoubleList.class)) {
            return new ConfigDoubleListField(owner, field, mapName, (ConfigDoubleList) annotation);
        } else if (atype.equals(ConfigExtendedMaterial.class)) {
            return new ConfigExtendedMaterialField(owner, field, mapName, (ConfigExtendedMaterial) annotation);
        } else if (atype.equals(ConfigInt.class)) {
            return new ConfigIntField(owner, field, mapName, (ConfigInt) annotation);
        } else if (atype.equals(ConfigIntList.class)) {
            return new ConfigIntListField(owner, field, mapName, (ConfigIntList) annotation);
        } else if (atype.equals(ConfigItemStack.class)) {
            return new ConfigItemStackField(owner, field, mapName, (ConfigItemStack) annotation);
        } else if (atype.equals(ConfigLong.class)) {
            return new ConfigLongField(owner, field, mapName, (ConfigLong) annotation);
        } else if (atype.equals(ConfigMaterial.class)) {
            return new ConfigMaterialField(owner, field, mapName, (ConfigMaterial) annotation);
        } else if (atype.equals(ConfigMaterialMapMapMap.class)) {
            return new ConfigMaterialMapMapMapField(owner, field, mapName, (ConfigMaterialMapMapMap) annotation);
        } else if (atype.equals(ConfigMaterialSet.class)) {
            return new ConfigMaterialSetField(owner, field, mapName, (ConfigMaterialSet) annotation);
        } else if (atype.equals(ConfigString.class)) {
            return new ConfigStringField(owner, field, mapName, (ConfigString) annotation);
        } else if (atype.equals(ConfigStringList.class)) {
            return new ConfigStringListField(owner, field, mapName, (ConfigStringList) annotation);
        } else if (atype.equals(ConfigStringListMap.class)) {
            return new ConfigStringListMapField(owner, field, mapName, (ConfigStringListMap) annotation);
        } else if (atype.equals(ConfigVersion.class)) {
            if (owner != module) {
                throw new RuntimeException("@ConfigVersion can only be used inside the main module. This is a bug.");
            }
            if (fieldVersion != null) {
                throw new RuntimeException(
                    "There must be exactly one @ConfigVersion field! (found multiple). This is a bug."
                );
            }
            return fieldVersion = new ConfigVersionField(owner, field, mapName, (ConfigVersion) annotation);
        } else {
            throw new RuntimeException("Missing ConfigField handler for @" + atype.getName() + ". This is a bug.");
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
                module.log.severe("This config is for an older version of " + module.getName() + ".");
                module.log.severe("Please update your configuration. A new default configuration");
                module.log.severe("has been generated as 'config.yml.new'. Alternatively you can");
                module.log.severe("delete your configuration to have a new one generated next time.");

                generateFile(new File(module.getDataFolder(), "config.yml.new"), null);
            } else {
                module.log.severe("This config is for a future version of " + module.getName() + ".");
                module.log.severe("Please use the correct file for this version, or delete it and");
                module.log.severe("it will be regenerated next time the server is started.");
            }

            return false;
        }

        return true;
    }

    public void addSectionDescription(String yamlPath, String description) {
        sectionDescriptions.put(yamlPath, description);
    }

    @SuppressWarnings("unchecked")
    public void compile(Object owner, Function<String, String> mapName) {
        // Compile all annotated fields
        configFields.addAll(
            getAllFields(owner.getClass())
                .stream()
                .filter(this::hasConfigAnnotation)
                .map(f -> compileField(owner, f, mapName))
                .toList()
        );

        // Sort fields alphabetically, and by precedence (e.g., put a version last and lang first)
        Collections.sort(configFields);

        if (owner == module && fieldVersion == null) {
            throw new RuntimeException("There must be exactly one @ConfigVersion field! (found none). This is a bug.");
        }
    }

    private String indentStr(int level) {
        return "  ".repeat(level);
    }

    public void generateYaml(StringBuilder builder, YamlConfiguration existingCompatibleConfig) {
        builder.append("# vim: set tabstop=2 softtabstop=0 expandtab shiftwidth=2:\n");
        builder.append("# This config file will automatically be updated, as long\n");
        builder.append("# as there are no incompatible changes between versions.\n");
        builder.append("# This means that additional comments will not be preserved!\n");

        // Use the version field as a neutral field in the root group
        ConfigField<?> lastField = fieldVersion;
        var indent = "";

        for (var f : configFields) {
            builder.append("\n");

            if (!ConfigField.sameGroup(lastField, f)) {
                final var newIndentLevel = f.groupCount();
                final var commonIndentLevel = ConfigField.commonGroupCount(lastField, f);

                // Build a full common section path
                var sectionPath = "";
                for (int i = 0; i < commonIndentLevel; ++i) {
                    sectionPath = Context.appendYamlPath(sectionPath, f.components()[i], ".");
                }

                // For each unopened section
                for (int i = commonIndentLevel; i < newIndentLevel; ++i) {
                    indent = indentStr(i);

                    // Get a full section path
                    sectionPath = Context.appendYamlPath(sectionPath, f.components()[i], ".");

                    // Append section description, if given.
                    final var sectionDesc = sectionDescriptions.get(sectionPath);
                    if (sectionDesc != null) {
                        final var descriptionWrapped = WordUtils.wrap(
                            sectionDesc,
                            Math.max(60, 80 - indent.length()),
                            "\n" + indent + "# ",
                            false
                        );
                        builder.append(indent);
                        builder.append("# ");
                        builder.append(descriptionWrapped);
                        builder.append("\n");
                    }

                    // Append section
                    final var sectionName = f.components()[i];
                    builder.append(indent);
                    builder.append(sectionName);
                    builder.append(":\n");
                }

                indent = indentStr(newIndentLevel);
            }

            // Append field YAML
            f.generateYaml(builder, indent, existingCompatibleConfig);
            lastField = f;
        }
    }

    public File standardFile() {
        return new File(module.getDataFolder(), "config.yml");
    }

    public boolean generateFile(File file, YamlConfiguration existingCompatibleConfig) {
        final var builder = new StringBuilder();
        generateYaml(builder, existingCompatibleConfig);
        final var content = builder.toString();

        // Save to tmp file, then move atomically to prevent corruption.
        final var tmpFile = new File(file.getAbsolutePath() + ".tmp");
        try {
            Files.writeString(tmpFile.toPath(), content);
        } catch (IOException e) {
            module.log.log(Level.SEVERE, "error while writing config file '" + file + "'", e);
            return false;
        }

        // Move atomically to prevent corruption.
        try {
            Files.move(
                tmpFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException e) {
            module.log.log(
                Level.SEVERE,
                "error while atomically replacing '" +
                file +
                "' with temporary file (very recent changes might be lost)!",
                e
            );
            return false;
        }

        return true;
    }

    public boolean reload(File file) {
        // Load file
        var yaml = YamlConfiguration.loadConfiguration(file);

        // Check version
        final var version = yaml.getLong("Version", -1);
        if (!verifyVersion(file, version)) {
            return false;
        }

        // Upgrade config to include all necessary keys (version-compatible extensions)
        final var tmpFile = new File(module.getDataFolder(), "config.yml.tmp");
        if (!generateFile(tmpFile, yaml)) {
            return false;
        }

        // Move atomically to prevent corruption.
        try {
            Files.move(
                tmpFile.toPath(),
                standardFile().toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException e) {
            module.log.log(
                Level.SEVERE,
                "error while atomically replacing '" +
                standardFile() +
                "' with updated version. Please manually resolve the conflict (new file is named '" +
                tmpFile +
                "')",
                e
            );
            return false;
        }

        // Load newly written file
        yaml = YamlConfiguration.loadConfiguration(file);

        try {
            // Check configuration for errors
            for (var f : configFields) {
                f.checkLoadable(yaml);
            }

            for (var f : configFields) {
                f.load(yaml);
            }
        } catch (YamlLoadException e) {
            module.log.log(Level.SEVERE, "error while loading '" + file.getName() + "'", e);
            return false;
        }

        return true;
    }

    public void registerMetrics(Metrics metrics) {
        // Track config values. Fields automatically know whether they want to be tracked or not via
        // the annotation.
        // By default, annotations use sensible defaults, so e.g., no strings will be tracked
        // automatically, except
        // when explicitly requested (e.g., language).
        for (var f : configFields) {
            f.registerMetrics(metrics);
        }
    }
}