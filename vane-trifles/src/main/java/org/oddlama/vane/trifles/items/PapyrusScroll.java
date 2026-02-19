package org.oddlama.vane.trifles.items;

import java.util.EnumSet;
import org.bukkit.Material;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "papyrus_scroll", base = Material.PAPER, modelData = 0x76000f, version = 1)
public class PapyrusScroll extends CustomItem<Trifles> {

    public PapyrusScroll(Context<Trifles> context) {
        super(context);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("RPP", "PEP", "PPG")
                .setIngredient('P', Material.PAPER)
                .setIngredient('R', Material.RABBIT_HIDE)
                .setIngredient('E', Material.ECHO_SHARD)
                .setIngredient('G', Material.GOLD_NUGGET)
                .result(key().toString())
        );
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE);
    }
}
