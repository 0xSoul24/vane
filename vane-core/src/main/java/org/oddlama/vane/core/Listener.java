package org.oddlama.vane.core;

import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

public class Listener<T extends Module<T>> extends ModuleComponent<T> implements org.bukkit.event.Listener {

    public Listener(Context<T> context) {
        super(context);
    }

    @Override
    protected void onEnable() {
        getModule().registerListener(this);
    }

    @Override
    protected void onDisable() {
        getModule().unregisterListener(this);
    }
}
