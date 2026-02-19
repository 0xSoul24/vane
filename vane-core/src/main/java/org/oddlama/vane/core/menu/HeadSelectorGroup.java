package org.oddlama.vane.core.menu;

import org.bukkit.Material;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;

public class HeadSelectorGroup extends ModuleComponent<Core> {

    @LangMessage
    public TranslatedMessage langTitle;

    @LangMessage
    public TranslatedMessage langFilterTitle;

    public TranslatedItemStack<?> itemSelectHead;

    public HeadSelectorGroup(Context<Core> context) {
        super(context.namespace("HeadSelector", "Menu configuration for the head selector menu."));
        itemSelectHead = new TranslatedItemStack<>(
            getContext(),
            "SelectHead",
            Material.BARRIER,
            1,
            "Used to represent a head in the head library."
        );
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
