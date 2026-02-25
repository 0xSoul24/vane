package org.oddlama.vane.core.config.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice.MaterialChoice
import org.bukkit.inventory.SmithingTransformRecipe
import org.oddlama.vane.util.ItemUtil

class SmithingRecipeDefinition(name: String?) : RecipeDefinition(name) {
    private var base: String? = null
    private var addition: String? = null
    private var copyNbt = false
    private var result: String? = null

    fun base(base: String?): SmithingRecipeDefinition {
        this.base = base
        return this
    }

    fun base(tag: Tag<*>): SmithingRecipeDefinition {
        return base("#" + tag.key())
    }

    fun base(material: Material): SmithingRecipeDefinition {
        return base(material.key().toString())
    }

    fun addition(addition: String?): SmithingRecipeDefinition {
        this.addition = addition
        return this
    }

    fun copyNbt(copyNbt: Boolean): SmithingRecipeDefinition {
        this.copyNbt = copyNbt
        return this
    }

    fun addition(tag: Tag<*>): SmithingRecipeDefinition {
        return addition("#" + tag.key())
    }

    fun addition(material: Material): SmithingRecipeDefinition {
        return addition(material.key().toString())
    }

    fun result(result: String?): SmithingRecipeDefinition {
        this.result = result
        return this
    }

    override fun toDict(): Any {
        val dict = HashMap<String?, Any?>()
        dict["Base"] = this.base
        dict["Addition"] = this.addition
        dict["CopyNbt"] = this.copyNbt
        dict["Result"] = this.result
        dict["Type"] = "smithing"
        return dict
    }

    override fun fromDict(dict: Any?): RecipeDefinition {
        require(dict is MutableMap<*, *>) { "Invalid smithing recipe dictionary: Argument must be a Map<String, Object>!" }
        val baseObj = if (dict.containsKey("Base")) dict["Base"] else dict["base"]
        if (baseObj is String) {
            this.base = baseObj
        } else {
            throw IllegalArgumentException("Invalid smithing recipe dictionary: base must be a string")
        }

        val additionObj = if (dict.containsKey("Addition")) dict["Addition"] else dict["addition"]
        if (additionObj is String) {
            this.addition = additionObj
        } else {
            throw IllegalArgumentException("Invalid smithing recipe dictionary: addition must be a string")
        }

        val copyNbtObj = if (dict.containsKey("CopyNbt")) dict["CopyNbt"] else dict["copyNbt"]
        if (copyNbtObj is Boolean) {
            this.copyNbt = copyNbtObj
        } else {
            throw IllegalArgumentException("Invalid smithing recipe dictionary: copyNbt must be a bool")
        }

        val resultObj = if (dict.containsKey("Result")) dict["Result"] else dict["result"]
        if (resultObj is String) {
            this.result = resultObj
        } else {
            throw IllegalArgumentException("Invalid smithing recipe dictionary: result must be a string")
        }

        return this
    }

    override fun toRecipe(baseKey: NamespacedKey?): Recipe {
        val bk = baseKey ?: throw IllegalArgumentException("baseKey cannot be null")
        val baseDef = base ?: throw IllegalArgumentException("Invalid smithing recipe: base must be set")
        val additionDef = addition ?: throw IllegalArgumentException("Invalid smithing recipe: addition must be set")
        val resultDef = result ?: throw IllegalArgumentException("Invalid smithing recipe: result must be set")

        return SmithingTransformRecipe(
            key(bk),
            ItemUtil.itemstackFromString(resultDef).getLeft()!!,
            MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
            recipeChoice(baseDef),
            recipeChoice(additionDef),
            copyNbt
        )
    }
}
