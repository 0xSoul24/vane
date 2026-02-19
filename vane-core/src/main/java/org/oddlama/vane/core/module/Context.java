package org.oddlama.vane.core.module;

import java.io.IOException;
import java.util.function.Consumer;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONObject;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

/**
 * A ModuleContext is an association to a specific Module and also a grouping of config and language
 * variables with a common namespace.
 */
public interface Context<T extends Module<T>> {
    public static String appendYamlPath(String ns1, String ns2, String separator) {
        if (ns1.isEmpty()) {
            return ns2;
        }
        return ns1 + separator + ns2;
    }

    /** create a sub-context namespace */
    public default ModuleContext<T> namespace(String name) {
        return new ModuleContext<T>(this, name, null, ".");
    }

    /** create a sub-context namespace */
    public default ModuleContext<T> namespace(String name, String description) {
        return new ModuleContext<T>(this, name, description, ".");
    }

    /** create a sub-context namespace */
    public default ModuleContext<T> namespace(String name, String description, String separator) {
        return new ModuleContext<T>(this, name, description, separator);
    }

    /** create a sub-context group */
    public default ModuleGroup<T> group(String group, String description) {
        return new ModuleGroup<T>(this, group, description);
    }

    public default ModuleGroup<T> group(String group, String description, boolean defaultEnabled) {
        final var g = new ModuleGroup<T>(this, group, description);
        g.configEnabledDef = defaultEnabled;
        return g;
    }

    /** create a sub-context group */
    public default ModuleGroup<T> groupDefaultDisabled(String group, String description) {
        final var g = group(group, description);
        g.configEnabledDef = false;
        return g;
    }

    /**
     * Compile the given component (processes lang and config definitions) and registers it for
     * onModuleEnable, onModuleDisable and onConfigChange events.
     */
    public void compile(ModuleComponent<T> component);

    public void addChild(Context<T> subcontext);

    public Context<T> getContext();

    public T getModule();

    public String yamlPath();

    public String variableYamlPath(String variable);

    public boolean enabled();

    public void enable();

    public void disable();

    public void configChange();

    public void generateResourcePack(final ResourcePackGenerator pack) throws IOException;

    public void forEachModuleComponent(final Consumer1<ModuleComponent<?>> f);

    public default void onModuleEnable() {}

    public default void onModuleDisable() {}

    public default void onConfigChange() {}

    public default void onGenerateResourcePack(final ResourcePackGenerator pack) throws IOException {}

    public default BukkitTask scheduleTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        return getModule().getServer().getScheduler().runTaskTimer(getModule(), task, delayTicks, periodTicks);
    }

    public default BukkitTask scheduleTask(Runnable task, long delayTicks) {
        return getModule().getServer().getScheduler().runTaskLater(getModule(), task, delayTicks);
    }

    public default BukkitTask scheduleNextTick(Runnable task) {
        return getModule().getServer().getScheduler().runTask(getModule(), task);
    }

    public default void addStorageMigrationTo(long to, String name, Consumer<JSONObject> migrator) {
        getModule().persistentStorageManager.addMigrationTo(to, name, migrator);
    }

    public default String storagePathOf(String field) {
        if (!field.startsWith("storage")) {
            throw new RuntimeException("Configuration fields must be prefixed storage. This is a bug.");
        }
        return variableYamlPath(field.substring("storage".length()));
    }

    public default void markPersistentStorageDirty() {
        getModule().markPersistentStorageDirty();
    }
}
