package org.oddlama.vane.core.config.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapelessRecipe
import org.oddlama.vane.util.ItemUtil

class ShapelessRecipeDefinition(name: String?) : RecipeDefinition(name) {
    private var ingredients: MutableList<String> = ArrayList()
    private var result: String? = null

    fun addIngredient(ingredient: String): ShapelessRecipeDefinition {
        this.ingredients.add(ingredient)
        return this
    }

    fun addIngredient(tag: Tag<*>): ShapelessRecipeDefinition {
        return addIngredient("#" + tag.key())
    }

    fun addIngredient(material: Material): ShapelessRecipeDefinition {
        return addIngredient(material.key().toString())
    }

    fun result(result: String?): ShapelessRecipeDefinition {
        this.result = result
        return this
    }

    override fun toDict(): Any {
        val dict = HashMap<String?, Any?>()
        dict["Type"] = "shapeless"
        dict["Ingredients"] = this.ingredients
        dict["Result"] = this.result
        return dict
    }

    override fun fromDict(dict: Any?): RecipeDefinition {
        require(dict is MutableMap<*, *>) { "Invalid shapeless recipe dictionary: Argument must be a Map<String, Object>!" }
        val ingredientsObj =
            if (dict.containsKey("Ingredients")) dict["Ingredients"] else dict["ingredients"]
        if (ingredientsObj is MutableList<*>) {
            this.ingredients = ingredientsObj.filterIsInstance<String>().toMutableList()
        } else {
            throw IllegalArgumentException(
                "Invalid shapeless recipe dictionary: ingredients must be a list of strings"
            )
        }

        val resultObj = if (dict.containsKey("Result")) dict["Result"] else dict["result"]
        if (resultObj is String) {
            this.result = resultObj
        } else {
            throw IllegalArgumentException("Invalid shapeless recipe dictionary: result must be a string")
        }

        return this
    }

    override fun toRecipe(baseKey: NamespacedKey?): Recipe {
        val bk = baseKey ?: throw IllegalArgumentException("baseKey cannot be null")
        val recipe = ShapelessRecipe(key(bk), ItemUtil.itemstackFromString(this.result!!).getLeft()!!)
        this.ingredients.forEach { i -> recipe.addIngredient(recipeChoice(i)) }
        return recipe
    }
}
