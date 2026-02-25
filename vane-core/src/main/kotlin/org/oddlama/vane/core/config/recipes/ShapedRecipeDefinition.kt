package org.oddlama.vane.core.config.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.oddlama.vane.util.ItemUtil

class ShapedRecipeDefinition(name: String?) : RecipeDefinition(name) {
    private var shape: MutableList<String> = ArrayList()
    private var ingredients: MutableMap<String?, String?> = HashMap()
    private var result: String? = null

    fun shape(vararg shape: String): ShapedRecipeDefinition {
        this.shape = mutableListOf(*shape)
        return this
    }

    fun setIngredient(id: Char, ingredient: String?): ShapedRecipeDefinition {
        this.ingredients["" + id] = ingredient
        return this
    }

    fun setIngredient(id: Char, tag: Tag<*>): ShapedRecipeDefinition {
        return setIngredient(id, "#" + tag.key())
    }

    fun setIngredient(id: Char, material: Material): ShapedRecipeDefinition {
        return setIngredient(id, material.key().toString())
    }

    fun result(result: String?): ShapedRecipeDefinition {
        this.result = result
        return this
    }

    override fun toDict(): Any {
        val dict = HashMap<String?, Any?>()
        dict["Type"] = "shaped"
        dict["Shape"] = this.shape
        dict["Ingredients"] = this.ingredients
        dict["Result"] = this.result
        return dict
    }

    override fun fromDict(dict: Any?): RecipeDefinition {
        require(dict is MutableMap<*, *>) { "Invalid shaped recipe dictionary: Argument must be a Map<String, Object>!" }
        val shapeObj = if (dict.containsKey("Shape")) dict["Shape"] else dict["shape"]
        if (shapeObj is MutableList<*>) {
            this.shape = shapeObj.filterIsInstance<String>().toMutableList()
            require(this.shape.size in 1..3) { "Invalid shaped recipe dictionary: shape must be a list of 1 to 3 strings" }
        } else {
            throw IllegalArgumentException("Invalid shaped recipe dictionary: shape must be a list of strings")
        }

        val ingredientsObj =
            if (dict.containsKey("Ingredients")) dict["Ingredients"] else dict["ingredients"]
        if (ingredientsObj is MutableMap<*, *>) {
            this.ingredients = ingredientsObj.entries
                .associate { e -> (e.key as String?) to (e.value as String?) }
                .toMutableMap()
        } else {
            throw IllegalArgumentException(
                "Invalid shaped recipe dictionary: ingredients must be a mapping of string to string"
            )
        }

        val resultObj = if (dict.containsKey("Result")) dict["Result"] else dict["result"]
        if (resultObj is String) {
            this.result = resultObj
        } else {
            throw IllegalArgumentException("Invalid shaped recipe dictionary: result must be a string")
        }

        return this
    }

    override fun toRecipe(baseKey: NamespacedKey?): Recipe {
        val bk = baseKey ?: throw IllegalArgumentException("baseKey cannot be null")
        val recipe = ShapedRecipe(key(bk), ItemUtil.itemstackFromString(this.result!!).getLeft()!!)
        recipe.shape(*this.shape.toTypedArray())
        this.ingredients.forEach { (name, definition) ->
            recipe.setIngredient(name!![0], recipeChoice(definition!!))
        }
        return recipe
    }
}
