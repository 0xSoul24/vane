package org.oddlama.vane.core.config.loot

import org.oddlama.vane.core.config.ConfigDictSerializable
import java.util.Locale

/**
 * Config-serializable list wrapper for [LootDefinition] entries.
 */
class LootTableList : ConfigDictSerializable {
    /** Stored loot definitions. */
    private var tables: MutableList<LootDefinition?> = mutableListOf()

    /** Returns the mutable loot definition list. */
    fun tables(): MutableList<LootDefinition?> = tables

    /** Serializes loot definitions to dictionary form. */
    override fun toDict(): MutableMap<String, Any> =
        tables.filterNotNull().associateTo(mutableMapOf()) { t -> toYamlName(t.name)!! to (t.serialize() as Any) }

    /** Loads loot definitions from dictionary form. */
    override fun fromDict(dict: MutableMap<String, Any>) {
        tables.clear()
        dict.entries.forEach { (name, raw) ->
            val internalName = if (name.isEmpty()) name else fromYamlName(name)
            tables.add(LootDefinition.deserialize(internalName, raw))
        }
    }

    /**
     * Construction and name-mapping helpers.
     */
    companion object {
        @JvmStatic
        /** Creates a [LootTableList] from vararg definitions. */
        fun of(vararg defs: LootDefinition?): LootTableList =
            LootTableList().also { it.tables = defs.toMutableList() }

        /** Maps internal loot names to preferred YAML display names. */
        private fun toYamlName(s: String?): String? {
            if (s.isNullOrEmpty()) return s
            return when (s.lowercase(Locale.getDefault())) {
                "generic"          -> "Generic"
                "terralith_generic"-> "TerralithGeneric"
                "terralith_rare"   -> "TerralithRare"
                "ancientcity"      -> "AncientCity"
                "bastion"          -> "Bastion"
                else               -> s
            }
        }

        /** Maps YAML display names back to internal loot names. */
        private fun fromYamlName(s: String?): String? {
            if (s.isNullOrEmpty()) return s
            return when (s.lowercase(Locale.getDefault()).replace("[_\\s]".toRegex(), "")) {
                "generic"          -> "generic"
                "terralithgeneric" -> "terralith_generic"
                "terralithrare"    -> "terralith_rare"
                "ancientcity"      -> "ancientcity"
                "bastion"          -> "bastion"
                else               -> s
            }
        }
    }
}
