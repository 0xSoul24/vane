package org.oddlama.vane.core.config.recipes

import org.bukkit.NamespacedKey
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigDict
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent
import java.util.function.Consumer
import java.util.function.Supplier

class Recipes<T : Module<T?>?> @JvmOverloads constructor(
    context: Context<T?>?,
    private val baseRecipeKey: NamespacedKey,
    private val defRecipes: Supplier<RecipeList?>,
    private val desc: String? = "The associated recipes. This is a map of recipe name to recipe definitions."
) : ModuleComponent<T?>(context) {
    @ConfigBoolean(
        def = true,
        desc = "Whether these recipes should be registered at all. Set to false to quickly disable all associated recipes."
    )
    var configRegisterRecipes: Boolean = false

    @ConfigDict(cls = RecipeList::class, desc = "")
    private var configRecipes: RecipeList? = null

    fun configRecipesDef(): RecipeList? {
        return defRecipes.get()
    }

    fun configRecipesDesc(): String? {
        return desc
    }

    override fun onConfigChange() {
        // Recipes are processed in onConfigChange and not in onModuleDisable() / onModuleEnable(),
        // as the current recipes need to be removed even if we are disabled afterward.
        configRecipes!!.recipes().forEach(Consumer { recipe: RecipeDefinition? ->
            module!!.server.removeRecipe(recipe!!.key(baseRecipeKey))
        })
        if (enabled() && configRegisterRecipes) {
            configRecipes!!
                .recipes()
                .forEach(Consumer { recipe: RecipeDefinition? ->
                    module!!.server.addRecipe(recipe!!.toRecipe(baseRecipeKey))
                })
        }
    }

    override fun onEnable() {}

    override fun onDisable() {}
}
