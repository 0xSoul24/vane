package org.oddlama.vane.core.config.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.RecipeChoice.ExactChoice
import org.bukkit.inventory.RecipeChoice.MaterialChoice
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.MaterialUtil.materialFrom
import org.oddlama.vane.util.StorageUtil.namespacedKey
import java.lang.reflect.Modifier

abstract class RecipeDefinition(var name: String?) {
    fun name(): String? {
        return name
    }

    fun key(baseKey: NamespacedKey): NamespacedKey {
        return namespacedKey(baseKey.namespace(), baseKey.value() + "." + name)
    }

    abstract fun toRecipe(baseKey: NamespacedKey?): Recipe?

    abstract fun toDict(): Any?

    abstract fun fromDict(dict: Any?): RecipeDefinition?

    companion object {
        @JvmStatic
        fun fromDict(name: String?, dict: Any): RecipeDefinition {
            require(dict is MutableMap<*, *>) { "Invalid recipe dictionary: Argument must be a Map<String, Object>, but is " + dict.javaClass + "!" }
            val typeObj = if (dict.containsKey("Type")) dict["Type"] else dict["type"]
            require(typeObj is String) { "Invalid recipe dictionary: recipe type must exist and be a string!" }

            when (typeObj) {
                "shaped" -> return ShapedRecipeDefinition(name).fromDict(dict)
                "shapeless" -> return ShapelessRecipeDefinition(name).fromDict(dict)
                "blasting", "furnace", "campfire", "smoking" -> return CookingRecipeDefinition(name, typeObj).fromDict(
                    dict
                )

                "smithing" -> return SmithingRecipeDefinition(name).fromDict(dict)
                "stonecutting" -> return StonecuttingRecipeDefinition(name).fromDict(dict)
                else -> {}
            }

            throw IllegalArgumentException("Unknown recipe type '$typeObj'")
        }

        @JvmStatic
        fun recipeChoice(definition: String): RecipeChoice {
            var definition = definition
            definition = definition.trim()

            // Try a material #tag
            if (definition.startsWith("#")) {
                for (f in Tag::class.java.getDeclaredFields()) {
                    if (Modifier.isStatic(f.modifiers) && f.type == Tag::class.java) {
                        try {
                            val tag = f.get(null) as Tag<*>?
                                ?: // System.out.println("warning: " + f + " has no associated key! It
                                // therefore cannot be used in custom recipes.");
                                continue
                            if (tag.key().toString() == definition.substring(1)) {
                                @Suppress("UNCHECKED_CAST")
                                return MaterialChoice(tag as Tag<Material>)
                            }
                        } catch (e: IllegalArgumentException) {
                            throw IllegalArgumentException("Invalid material tag: $definition")
                        } catch (e: IllegalAccessException) {
                            throw IllegalArgumentException("Invalid material tag: $definition")
                        }
                    }
                }
                throw IllegalArgumentException("Unknown material tag: $definition")
            }

            // Tuple of materials
            if (definition.startsWith("(") && definition.endsWith(")")) {
                val parts = definition.substring(1, definition.length - 1)
                    .split(",")
                    .map { it.trim() }
                    .map { key ->
                        val mat = materialFrom(NamespacedKey.fromString(key)!!)
                        requireNotNull(mat) { "Unknown material (only normal materials are allowed in tags): $key" }
                        mat
                    }
                return MaterialChoice(parts)
            }

            // Check if the amount is included
            val mult = definition.indexOf('*')
            var amount = 1
            if (mult != -1) {
                val amountStr = definition.substring(0, mult).trim()
                try {
                    amount = amountStr.toInt()
                    if (amount <= 0) {
                        amount = 1
                    }

                    // Remove amount from definition for parsing
                    definition = definition.substring(mult + 1).trim()
                } catch (e: NumberFormatException) {
                }
            }

            // Exact choice of itemstack including NBT
            val itemStackAndIsSimpleMat = ItemUtil.itemstackFromString(definition)
            val itemStack = itemStackAndIsSimpleMat.getLeft()!!
            val isSimpleMat = itemStackAndIsSimpleMat.getRight() ?: false
            if (isSimpleMat && amount == 1) {
                return MaterialChoice(itemStack.type)
            }

            itemStack.amount = amount
            return ExactChoice(itemStack)
        }
    }
}
