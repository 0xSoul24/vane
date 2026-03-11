package org.oddlama.vane.core.config.recipes

import org.oddlama.vane.core.config.ConfigDictSerializable
import java.util.*

class RecipeList(private var recipes: MutableList<RecipeDefinition?> = mutableListOf()) : ConfigDictSerializable {

    fun recipes(): MutableList<RecipeDefinition?> = recipes

    override fun toDict(): MutableMap<String, Any> =
        recipes.filterNotNull().associateTo(LinkedHashMap()) { r -> toYamlName(r.name)!! to (r.toDict() as Any) }

    override fun fromDict(dict: MutableMap<String, Any>) {
        recipes.clear()
        dict.entries.forEach { (key, value) ->
            val internalName = if (key.isEmpty()) key else fromYamlName(key)
            recipes.add(RecipeDefinition.fromDict(internalName, value))
        }
    }

    companion object {
        @JvmStatic
        fun of(vararg defs: RecipeDefinition?): RecipeList = RecipeList(defs.toMutableList())

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
