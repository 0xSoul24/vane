package org.oddlama.vane.regions.menu;

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
import org.oddlama.vane.regions.Regions;

public class EnterRegionGroupNameMenu extends ModuleComponent<Regions> {

    @LangMessage
    public TranslatedMessage langTitle;

    @ConfigMaterial(def = Material.GLOBE_BANNER_PATTERN, desc = "The item used to name region groups.")
    public Material configMaterial;

    public EnterRegionGroupNameMenu(Context<Regions> context) {
        super(context.namespace("EnterRegionGroupName"));
    }

    public Menu create(final Player player, final Function2<Player, String, ClickResult> onClick) {
        return create(player, "Group", onClick);
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
