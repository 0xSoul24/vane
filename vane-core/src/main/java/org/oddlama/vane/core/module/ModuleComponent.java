package org.oddlama.vane.core.module;

import java.io.IOException;
import java.util.function.Consumer;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONObject;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

public abstract class ModuleComponent<T extends Module<T>> {

    private Context<T> context = null;

    public ModuleComponent(Context<T> context) {
        if (context == null) {
            // Delay until setContext is called.
            return;
        }
        setContext(context);
    }

    public void setContext(Context<T> context) {
        if (this.context != null) {
            throw new RuntimeException("Cannot replace existing context! This is a bug.");
        }
        this.context = context;
        context.compile(this);
    }

    public Context<T> getContext() {
        return context;
    }

    public T getModule() {
        return context.getModule();
    }

    public boolean enabled() {
        return context.enabled();
    }

    protected abstract void onEnable();

    protected abstract void onDisable();

    protected void onConfigChange() {}

    protected void onGenerateResourcePack(final ResourcePackGenerator pack) throws IOException {}

    public final BukkitTask scheduleTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        return context.scheduleTaskTimer(task, delayTicks, periodTicks);
    }

    public final BukkitTask scheduleTask(Runnable task, long delayTicks) {
        return context.scheduleTask(task, delayTicks);
    }

    public final BukkitTask scheduleNextTick(Runnable task) {
        return context.scheduleNextTick(task);
    }

    public final void addStorageMigrationTo(long to, String name, Consumer<JSONObject> migrator) {
        context.addStorageMigrationTo(to, name, migrator);
    }

    public final String storagePathOf(String field) {
        return context.storagePathOf(field);
    }

    public final void markPersistentStorageDirty() {
        context.markPersistentStorageDirty();
    }

    public final NamespacedKey namespacedKey(String value) {
        return new NamespacedKey(getModule(), getContext().variableYamlPath(value));
    }
}
