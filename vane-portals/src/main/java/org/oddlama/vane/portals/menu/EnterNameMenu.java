package org.oddlama.vane.portals.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Menu;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.Portals;

public class EnterNameMenu extends ModuleComponent<Portals> {

    @LangMessage
    public TranslatedMessage langTitle;

    @ConfigMaterial(def = Material.ENDER_PEARL, desc = "The item used to name portals.")
    public Material configMaterial;

    public EnterNameMenu(Context<Portals> context) {
        super(context.namespace("EnterName"));
    }

    public Menu create(final Player player, final Function2<Player, String, ClickResult> onClick) {
        return create(player, "Name", onClick);
    }

    public Menu create(
        final Player player,
        final String defaultName,
        final Function2<Player, String, ClickResult> onClick
    ) {
        return MenuFactory.anvilStringInput(
            getContext(),
            player,
            langTitle.str(),
            new ItemStack(configMaterial),
            defaultName,
            (p, menu, name) -> {
                menu.close(p);
                return onClick.apply(p, name);
            }
        );
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
