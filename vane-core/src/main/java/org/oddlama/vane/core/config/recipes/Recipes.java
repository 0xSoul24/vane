package org.oddlama.vane.core.config.recipes;

import java.util.function.Supplier;
import org.bukkit.NamespacedKey;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigDict;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

public class Recipes<T extends Module<T>> extends ModuleComponent<T> {

    private final NamespacedKey baseRecipeKey;

    @ConfigBoolean(
        def = true,
        desc = "Whether these recipes should be registered at all. Set to false to quickly disable all associated recipes."
    )
    public boolean configRegisterRecipes;

    @ConfigDict(cls = RecipeList.class, desc = "")
    private RecipeList configRecipes;

    private Supplier<RecipeList> defRecipes;
    private String desc;

    public Recipes(
        final Context<T> context,
        final NamespacedKey baseRecipeKey,
        final Supplier<RecipeList> defRecipes
    ) {
        this(
            context,
                baseRecipeKey,
                defRecipes,
            "The associated recipes. This is a map of recipe name to recipe definitions."
        );
    }

    public Recipes(
        final Context<T> context,
        final NamespacedKey baseRecipeKey,
        final Supplier<RecipeList> defRecipes,
        final String desc
    ) {
        super(context);
        this.baseRecipeKey = baseRecipeKey;
        this.defRecipes = defRecipes;
        this.desc = desc;
    }

    public RecipeList configRecipesDef() {
        return defRecipes.get();
    }

    public String configRecipesDesc() {
        return desc;
    }

    @Override
    public void onConfigChange() {
        // Recipes are processed in onConfigChange and not in onModuleDisable() / onModuleEnable(),
        // as the current recipes need to be removed even if we are disabled afterward.
        configRecipes.recipes().forEach(recipe -> getModule().getServer().removeRecipe(recipe.key(baseRecipeKey)));
        if (enabled() && configRegisterRecipes) {
            configRecipes
                .recipes()
                .forEach(recipe -> getModule().getServer().addRecipe(recipe.toRecipe(baseRecipeKey)));
        }
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}
}
