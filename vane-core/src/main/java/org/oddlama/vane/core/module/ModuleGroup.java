package org.oddlama.vane.core.module;

import org.oddlama.vane.annotation.config.ConfigBoolean;

/**
 * A ModuleGroup is a ModuleContext that automatically adds an enabled variable with description to
 * the context. If the group is disabled, onModuleEnable() will not be called.
 */
public class ModuleGroup<T extends Module<T>> extends ModuleContext<T> {

    @ConfigBoolean(def = true, desc = "") // desc is set by #configEnabledDesc()
    public boolean configEnabled;

    public boolean configEnabledDef = true;
    private String configEnabledDesc;

    public boolean configEnabledDef() {
        return configEnabledDef;
    }

    public String configEnabledDesc() {
        return configEnabledDesc;
    }

    public ModuleGroup(Context<T> context, String group, String description) {
        this(context, group, description, true);
    }

    public ModuleGroup(Context<T> context, String group, String description, boolean compileSelf) {
        super(context, group, null, ".", false);
        this.configEnabledDesc = description;

        if (compileSelf) {
            compileSelf();
        }
    }

    @Override
    public boolean enabled() {
        return configEnabled;
    }

    @Override
    public void enable() {
        if (configEnabled) {
            super.enable();
        }
    }

    @Override
    public void disable() {
        if (configEnabled) {
            super.disable();
        }
    }
}
