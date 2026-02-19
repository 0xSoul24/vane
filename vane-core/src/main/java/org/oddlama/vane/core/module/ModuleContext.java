package org.oddlama.vane.core.module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

/**
 * A ModuleContext is an association to a specific Module and also a grouping of config and language
 * variables with a common namespace.
 */
public class ModuleContext<T extends Module<T>> implements Context<T> {

    protected Context<T> context;
    protected T module; // cache to not generate chains of getContext()
    protected String name;
    private List<Context<T>> subcontexts = new ArrayList<>();
    private List<ModuleComponent<T>> components = new ArrayList<>();
    private String description;
    private String separator;

    public ModuleContext(Context<T> context, String name, String description, String separator) {
        this(context, name, description, separator, true);
    }

    public ModuleContext(Context<T> context, String name, String description, String separator, boolean compileSelf) {
        this.context = context;
        this.module = context.getModule();
        this.name = name;
        this.description = description;
        this.separator = separator;

        if (compileSelf) {
            compileSelf();
        }
    }

    @Override
    public String yamlPath() {
        return Context.appendYamlPath(context.yamlPath(), name, separator);
    }

    public String variableYamlPath(String variable) {
        return Context.appendYamlPath(yamlPath(), variable, separator);
    }

    @Override
    public boolean enabled() {
        return context.enabled();
    }

    private void compileComponent(Object component) {
        module.langManager.compile(component, this::variableYamlPath);
        module.configManager.compile(component, this::variableYamlPath);
        if (description != null) {
            module.configManager.addSectionDescription(yamlPath(), description);
        }
        module.persistentStorageManager.compile(component, this::variableYamlPath);
    }

    protected void compileSelf() {
        // Compile localization and config fields
        compileComponent(this);
        context.addChild(this);
    }

    @Override
    public void compile(ModuleComponent<T> component) {
        components.add(component);
        compileComponent(component);
    }

    @Override
    public void addChild(Context<T> subcontext) {
        subcontexts.add(subcontext);
    }

    @Override
    public Context<T> getContext() {
        return context;
    }

    @Override
    public T getModule() {
        return module;
    }

    @Override
    public void enable() {
        onModuleEnable();
        for (var component : components) {
            component.onEnable();
        }
        for (var subcontext : subcontexts) {
            subcontext.enable();
        }
    }

    @Override
    public void disable() {
        for (int i = subcontexts.size() - 1; i >= 0; --i) {
            subcontexts.get(i).disable();
        }
        for (int i = components.size() - 1; i >= 0; --i) {
            components.get(i).onDisable();
        }
        onModuleDisable();
    }

    @Override
    public void configChange() {
        onConfigChange();
        for (var component : components) {
            component.onConfigChange();
        }
        for (var subcontext : subcontexts) {
            subcontext.configChange();
        }
    }

    @Override
    public void generateResourcePack(final ResourcePackGenerator pack) throws IOException {
        onGenerateResourcePack(pack);
        for (var component : components) {
            component.onGenerateResourcePack(pack);
        }
        for (var subcontext : subcontexts) {
            subcontext.generateResourcePack(pack);
        }
    }

    @Override
    public void forEachModuleComponent(final Consumer1<ModuleComponent<?>> f) {
        for (var component : components) {
            f.apply(component);
        }
        for (var subcontext : subcontexts) {
            subcontext.forEachModuleComponent(f);
        }
    }
}
