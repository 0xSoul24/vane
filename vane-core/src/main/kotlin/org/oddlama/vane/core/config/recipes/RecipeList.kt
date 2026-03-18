package org.oddlama.vane.core.config.recipes

import org.oddlama.vane.core.config.ConfigDictSerializable
import java.util.*

/**
 * Config-serializable list wrapper for recipe definitions.
 *
 * @param recipes initial recipe definitions.
 */
class RecipeList(private var recipes: MutableList<RecipeDefinition?> = mutableListOf()) : ConfigDictSerializable {

    /** Returns the mutable list of recipe definitions. */
    fun recipes(): MutableList<RecipeDefinition?> = recipes

    /** Serializes this recipe list to dictionary form. */
    override fun toDict(): MutableMap<String, Any> =
        recipes.filterNotNull().associateTo(LinkedHashMap()) { r -> toYamlName(r.name)!! to (r.toDict() as Any) }

    /** Loads recipe definitions from dictionary form. */
    override fun fromDict(dict: MutableMap<String, Any>) {
        recipes.clear()
        dict.entries.forEach { (key, value) ->
            val internalName = if (key.isEmpty()) key else fromYamlName(key)
            recipes.add(RecipeDefinition.fromDict(internalName, value))
        }
    }

    /**
     * Construction and name-mapping helpers.
     */
    companion object {
        @JvmStatic
                /** Creates a [RecipeList] from vararg definitions. */
        fun of(vararg defs: RecipeDefinition?): RecipeList = RecipeList(defs.toMutableList())

        /** Maps internal recipe names to preferred YAML display names. */
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

        /** Maps YAML display names back to internal recipe names. */
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
