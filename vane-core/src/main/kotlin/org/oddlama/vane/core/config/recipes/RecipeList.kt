package org.oddlama.vane.core.config.recipes

import org.oddlama.vane.core.config.ConfigDictSerializable
import java.util.*

class RecipeList : ConfigDictSerializable {
    private var recipes: MutableList<RecipeDefinition?> = ArrayList<RecipeDefinition?>()

    constructor()

    constructor(recipes: MutableList<RecipeDefinition?>) {
        this.recipes = recipes
    }

    fun recipes(): MutableList<RecipeDefinition?> {
        return recipes
    }

    // Mapear nombres especiales a PascalCase para YAML: 'generic' -> 'Generic', 'terralith_generic' -> 'TerralithGeneric', 'terralith_rare' -> 'TerralithRare', 'ancientcity' -> 'AncientCity', 'bastion' -> 'Bastion', 'from_shulker_box' -> 'FromShulkerBox'
    override fun toDict(): MutableMap<String?, Any?> {
        val map: MutableMap<String?, Any?> = LinkedHashMap()
        for (r in recipes) {
            if (r == null) continue
            map[toYamlName(r.name())] = r.toDict()
        }
        return map
    }

    override fun fromDict(dict: MutableMap<String?, Any?>?) {
        recipes.clear()
        if (dict == null) return
        for (e in dict.entries) {
            val key = e.key
            val internalName = if (key.isNullOrEmpty()) key else fromYamlName(key)
            recipes.add(RecipeDefinition.fromDict(internalName, e.value!!))
        }
    }

    companion object {
        @JvmStatic
        fun of(vararg defs: RecipeDefinition?): RecipeList {
            val rl = RecipeList()
            rl.recipes = defs.toMutableList()
            return rl
        }

        private fun toYamlName(s: String?): String? {
            if (s.isNullOrEmpty()) return s
            val low = s.lowercase(Locale.getDefault())
            return when (low) {
                "generic" -> "Generic"
                "terralith_generic" -> "TerralithGeneric"
                "terralith_rare" -> "TerralithRare"
                "ancientcity" -> "AncientCity"
                "bastion" -> "Bastion"
                "from_shulker_box" -> "FromShulkerBox"
                else -> s
            }
        }

        private fun fromYamlName(s: String?): String? {
            if (s.isNullOrEmpty()) return s
            val low = s.lowercase(Locale.getDefault()).replace("[_\\s]".toRegex(), "")
            return when (low) {
                "generic" -> "generic"
                "terralithgeneric" -> "terralith_generic"
                "terralithrare" -> "terralith_rare"
                "ancientcity" -> "ancientcity"
                "bastion" -> "bastion"
                "fromshulkerbox" -> "from_shulker_box"
                else -> s
            }
        }
    }
}
