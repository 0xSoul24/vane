package org.oddlama.vane.core.config.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.*
import org.oddlama.vane.util.ItemUtil

class CookingRecipeDefinition(name: String?, private val type: String) : RecipeDefinition(name) {
    private var input: String? = null
    private var result: String? = null
    private var experience = 0.0f
    private var cookingTime = 10

    init {
        when (this.type) {
            "blasting", "furnace", "campfire", "smoking" -> {}
            else -> throw IllegalArgumentException("Invalid cooking recipe: Unknown type '" + this.type + "'")
        }
    }

    fun input(input: String?): CookingRecipeDefinition {
        this.input = input
        return this
    }

    fun input(tag: Tag<*>): CookingRecipeDefinition {
        return input("#" + tag.key())
    }

    fun input(material: Material): CookingRecipeDefinition {
        return input(material.key().toString())
    }

    fun result(result: String?): CookingRecipeDefinition {
        this.result = result
        return this
    }

    override fun toDict(): Any {
        val dict = HashMap<String?, Any?>()
        dict["CookingTime"] = this.cookingTime
        dict["Experience"] = this.experience
        dict["Input"] = this.input
        dict["Result"] = this.result
        dict["Type"] = type
        return dict
    }

    override fun fromDict(dict: Any?): RecipeDefinition {
        require(dict is MutableMap<*, *>) { "Invalid $type recipe dictionary: Argument must be a Map<String, Object>!" }
        val inputObj = if (dict.containsKey("Input")) dict["Input"] else dict["input"]
        if (inputObj is String) {
            this.input = inputObj
        } else {
            throw IllegalArgumentException("Invalid $type recipe dictionary: input must be a string")
        }

        val resultObj = if (dict.containsKey("Result")) dict["Result"] else dict["result"]
        if (resultObj is String) {
            this.result = resultObj
        } else {
            throw IllegalArgumentException("Invalid $type recipe dictionary: result must be a string")
        }

        val experienceObj =
            if (dict.containsKey("Experience")) dict["Experience"] else dict["experience"]
        if (experienceObj is Float) {
            this.experience = experienceObj
        } else {
            throw IllegalArgumentException("Invalid $type recipe dictionary: experience must be a float")
        }

        val cookingTimeObj =
            if (dict.containsKey("CookingTime")) dict["CookingTime"] else dict["cookingTime"]
        if (cookingTimeObj is Int) {
            this.cookingTime = cookingTimeObj
        } else {
            throw IllegalArgumentException("Invalid $type recipe dictionary: cookingTime must be a int")
        }

        return this
    }

    override fun toRecipe(baseKey: NamespacedKey?): Recipe? {
        val bk = baseKey ?: throw IllegalArgumentException("baseKey cannot be null")
        val out = ItemUtil.itemstackFromString(this.result!!).getLeft()!!
        val `in` = recipeChoice(input!!)
        return when (this.type) {
            "blasting" -> BlastingRecipe(key(bk), out, `in`, experience, cookingTime)
            "furnace" -> FurnaceRecipe(key(bk), out, `in`, experience, cookingTime)
            "campfire" -> CampfireRecipe(key(bk), out, `in`, experience, cookingTime)
            "smoking" -> SmokingRecipe(key(bk), out, `in`, experience, cookingTime)
            else -> throw IllegalArgumentException("Invalid cooking recipe: Unknown type '" + this.type + "'")
        }
    }
}
