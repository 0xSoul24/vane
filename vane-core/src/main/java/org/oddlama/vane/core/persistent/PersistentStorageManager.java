package org.oddlama.vane.core.persistent;

import static org.reflections.ReflectionUtils.getAllFields;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import org.json.JSONObject;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.module.Module;

public class PersistentStorageManager {

    public class Migration {

        public long to;
        public String name;
        public Consumer<JSONObject> migrator;

        public Migration(long to, String name, Consumer<JSONObject> migrator) {
            this.to = to;
            this.name = name;
            this.migrator = migrator;
        }
    }

    private List<PersistentField> persistentFields = new ArrayList<>();
    private List<Migration> migrations = new ArrayList<>();
    Module<?> module;
    boolean isLoaded = false;

    public PersistentStorageManager(Module<?> module) {
        this.module = module;
        compile(module, s -> s);
    }

    private boolean hasPersistentAnnotation(Field field) {
        for (var a : field.getAnnotations()) {
            if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.persistent.Persistent")) {
                return true;
            }
        }
        return false;
    }

    private void assertFieldPrefix(Field field) {
        if (!field.getName().startsWith("storage")) {
            throw new RuntimeException("Configuration fields must be prefixed storage. This is a bug.");
        }
    }

    private PersistentField compileField(Object owner, Field field, Function<String, String> mapName) {
        assertFieldPrefix(field);

        // Get the annotation
        Annotation annotation = null;
        for (var a : field.getAnnotations()) {
            if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.persistent.Persistent")) {
                if (annotation == null) {
                    annotation = a;
                } else {
                    throw new RuntimeException("Persistent fields must have exactly one @Persistent annotation.");
                }
            }
        }
        assert annotation != null;
        final var atype = annotation.annotationType();

        // Return a correct wrapper object
        if (atype.equals(Persistent.class)) {
            return new PersistentField(owner, field, mapName);
        } else {
            throw new RuntimeException("Missing PersistentField handler for @" + atype.getName() + ". This is a bug.");
        }
    }

    @SuppressWarnings("unchecked")
    public void compile(Object owner, Function<String, String> mapName) {
        // Compile all annotated fields
        persistentFields.addAll(
            getAllFields(owner.getClass())
                .stream()
                .filter(this::hasPersistentAnnotation)
                .map(f -> compileField(owner, f, mapName))
                .toList()
        );
    }

    public void addMigrationTo(long to, String name, Consumer<JSONObject> migrator) {
        migrations.add(new Migration(to, name, migrator));
    }

    @SuppressWarnings("unchecked")
    public boolean load(File file) {
        if (!file.exists() && isLoaded) {
            module.log.severe("Cannot reload persistent storage from nonexistent file '" + file.getName() + "'");
            return false;
        }

        // Reset loaded status
        isLoaded = false;

        final JSONObject json;
        if (file.exists()) {
            // Open file and read json
            try {
                json = new JSONObject(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                module.log.severe("error while loading persistent data from '" + file.getName() + "':");
                module.log.severe(e.getMessage());
                return false;
            }
        } else {
            json = new JSONObject();
        }

        // Check version and migrate if necessary
        final var versionPath = module.storagePathOf("storageVersion");
        final var version = Long.parseLong(json.optString(versionPath, "0"));
        final var neededVersion = module.annotation.storageVersion();
        if (version != neededVersion && migrations.size() > 0) {
            module.log.info("Persistent storage is out of date.");
            module.log.info("§dMigrating storage from version §b" + version + " → " + neededVersion + "§d:");

            // Sort migrations by target version,
            // then apply new migrations in order.
            migrations
                .stream()
                .filter(m -> m.to >= version)
                .sorted((a, b) -> Long.compare(a.to, b.to))
                .forEach(m -> {
                    module.log.info("  → §b" + m.to + "§r : Applying migration '§a" + m.name + "§r'");
                    m.migrator.accept(json);
                });
        }

        // Overwrite new version
        json.put(versionPath, String.valueOf(neededVersion));

        try {
            for (final var f : persistentFields) {
                // If we have just initialized a new json object, we only load values that
                // have defined keys (e.g., from initialization migrations)
                if (version == 0 && !json.has(f.path())) {
                    continue;
                }

                f.load(json);
            }
        } catch (IOException e) {
            module.log.log(Level.SEVERE, "error while loading persistent variables from '" + file.getName() + "'", e);
            return false;
        }

        isLoaded = true;
        return true;
    }

    public void save(File file) {
        if (!isLoaded) {
            // Don't save if never loaded or a previous load was faulty.
            return;
        }

        // Create JSON with whole content
        final var json = new JSONObject();

        // Save version
        final var versionPath = module.storagePathOf("storageVersion");
        json.put(versionPath, String.valueOf(module.annotation.storageVersion()));

        // Save fields
        for (final var f : persistentFields) {
            try {
                f.save(json);
            } catch (IOException e) {
                module.log.log(Level.SEVERE, "error while serializing persistent data!", e);
            }
        }

        // Save to tmp file, then move atomically to prevent corruption.
        final var tmpFile = new File(file.getAbsolutePath() + ".tmp");
        try {
            Files.writeString(tmpFile.toPath(), json.toString());
        } catch (IOException e) {
            module.log.log(Level.SEVERE, "error while saving persistent data to temporary file!", e);
            return;
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
        }
    }
}
