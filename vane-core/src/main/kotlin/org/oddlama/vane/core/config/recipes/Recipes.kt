package org.oddlama.vane.core.config.recipes

import org.bukkit.NamespacedKey
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigDict
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent

class Recipes<T : Module<T?>?> @JvmOverloads constructor(
    context: Context<T?>?,
    private val baseRecipeKey: NamespacedKey,
    private val defRecipes: () -> RecipeList?,
    private val desc: String? = "The associated recipes. This is a map of recipe name to recipe definitions."
) : ModuleComponent<T?>(context) {
    @ConfigBoolean(def = true, desc = "Whether these recipes should be registered at all. Set to false to quickly disable all associated recipes.")
    var configRegisterRecipes: Boolean = false

    @ConfigDict(cls = RecipeList::class, desc = "")
    private var configRecipes: RecipeList? = null

    fun configRecipesDef(): RecipeList? = defRecipes()
    fun configRecipesDesc(): String? = desc

    override fun onConfigChange() {
        configRecipes!!.recipes().forEach { recipe ->
            module!!.server.removeRecipe(recipe!!.key(baseRecipeKey))
        }
        if (enabled() && configRegisterRecipes) {
            configRecipes!!.recipes().forEach { recipe ->
                module!!.server.addRecipe(recipe!!.toRecipe(baseRecipeKey))
            }
        }
    }

    override fun onEnable() {}
    override fun onDisable() {}
}
