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

abstract class RecipeDefinition(val name: String?) {
    fun key(baseKey: NamespacedKey): NamespacedKey =
        namespacedKey(baseKey.namespace(), "${baseKey.value()}.$name")

    abstract fun toRecipe(baseKey: NamespacedKey?): Recipe?
    abstract fun toDict(): Any?
    abstract fun fromDict(dict: Any?): RecipeDefinition?

    companion object {
        @JvmStatic
        fun fromDict(name: String?, dict: Any): RecipeDefinition {
            require(dict is Map<*, *>) {
                "Invalid recipe dictionary: Argument must be a Map<String, Object>, but is ${dict.javaClass}!"
            }
            val typeObj = dict["Type"] ?: dict["type"]
            require(typeObj is String) { "Invalid recipe dictionary: recipe type must exist and be a string!" }

            return when (typeObj) {
                "shaped"     -> ShapedRecipeDefinition(name).fromDict(dict)
                "shapeless"  -> ShapelessRecipeDefinition(name).fromDict(dict)
                "blasting", "furnace", "campfire", "smoking" -> CookingRecipeDefinition(name, typeObj).fromDict(dict)
                "smithing"   -> SmithingRecipeDefinition(name).fromDict(dict)
                "stonecutting" -> StonecuttingRecipeDefinition(name).fromDict(dict)
                else -> throw IllegalArgumentException("Unknown recipe type '$typeObj'")
            }
        }

        @JvmStatic
        fun recipeChoice(definition: String): RecipeChoice {
            val trimmed = definition.trim()

            // Try a material #tag
            if (trimmed.startsWith("#")) {
                for (f in Tag::class.java.declaredFields) {
                    if (!Modifier.isStatic(f.modifiers) || f.type != Tag::class.java) continue
                    try {
                        val tag = f.get(null) as? Tag<*> ?: continue
                        if (tag.key.toString() == trimmed.substring(1)) {
                            @Suppress("UNCHECKED_CAST")
                            return MaterialChoice(tag as Tag<Material>)
                        }
                    } catch (_: IllegalAccessException) {
                        throw IllegalArgumentException("Invalid material tag: $trimmed")
                    }
                }
                throw IllegalArgumentException("Unknown material tag: $trimmed")
            }

            // Tuple of materials
            if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
                val parts = trimmed.substring(1, trimmed.length - 1)
                    .split(",")
                    .map { it.trim() }
                    .map { key ->
                        requireNotNull(materialFrom(NamespacedKey.fromString(key)!!)) {
                            "Unknown material (only normal materials are allowed in tags): $key"
                        }
                    }
                return MaterialChoice(parts)
            }

            // Check if amount is included: only treat '*' as separator when the prefix is a valid int
            val multIdx = trimmed.indexOf('*')
            val (amount, itemDef) = if (multIdx != -1) {
                val amt = trimmed.substring(0, multIdx).trim().toIntOrNull()
                if (amt != null) amt.coerceAtLeast(1) to trimmed.substring(multIdx + 1).trim()
                else 1 to trimmed
            } else {
                1 to trimmed
            }

            // Exact choice of itemstack including NBT
            val itemStackAndIsSimpleMat = ItemUtil.itemstackFromString(itemDef)
            val itemStack = itemStackAndIsSimpleMat.getLeft()!!
            val isSimpleMat = itemStackAndIsSimpleMat.getRight() ?: false
            if (isSimpleMat && amount == 1) return MaterialChoice(itemStack.type)

            itemStack.amount = amount
            return ExactChoice(itemStack)
        }
    }
}
