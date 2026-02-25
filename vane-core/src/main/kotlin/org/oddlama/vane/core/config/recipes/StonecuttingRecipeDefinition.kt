package org.oddlama.vane.core.config.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.StonecuttingRecipe
import org.oddlama.vane.util.ItemUtil

class StonecuttingRecipeDefinition(name: String?) : RecipeDefinition(name) {
    private var input: String? = null
    private var result: String? = null

    fun input(input: String?): StonecuttingRecipeDefinition {
        this.input = input
        return this
    }

    fun input(tag: Tag<*>): StonecuttingRecipeDefinition {
        return input("#" + tag.key())
    }

    fun input(material: Material): StonecuttingRecipeDefinition {
        return input(material.key().toString())
    }

    fun result(result: String?): StonecuttingRecipeDefinition {
        this.result = result
        return this
    }

    override fun toDict(): Any {
        val dict = HashMap<String?, Any?>()
        dict["Input"] = this.input
        dict["Result"] = this.result
        dict["Type"] = "stonecutting"
        return dict
    }

    override fun fromDict(dict: Any?): RecipeDefinition {
        require(dict is MutableMap<*, *>) { "Invalid stonecutting recipe dictionary: Argument must be a Map<String, Object>!" }
        val inputObj = if (dict.containsKey("Input")) dict["Input"] else dict["input"]
        if (inputObj is String) {
            this.input = inputObj
        } else {
            throw IllegalArgumentException("Invalid stonecutting recipe dictionary: input must be a string")
        }

        val resultObj = if (dict.containsKey("Result")) dict["Result"] else dict["result"]
        if (resultObj is String) {
            this.result = resultObj
        } else {
            throw IllegalArgumentException("Invalid stonecutting recipe dictionary: result must be a string")
        }

        return this
    }

    override fun toRecipe(baseKey: NamespacedKey?): Recipe {
        val bk = baseKey ?: throw IllegalArgumentException("baseKey cannot be null")
        val inputDef = input ?: throw IllegalArgumentException("Invalid stonecutting recipe: input must be set")
        val resultDef = result ?: throw IllegalArgumentException("Invalid stonecutting recipe: result must be set")
        val out = ItemUtil.itemstackFromString(resultDef).getLeft()!!
        val `in` = recipeChoice(inputDef)
        return StonecuttingRecipe(key(bk), out, `in`)
    }
}
