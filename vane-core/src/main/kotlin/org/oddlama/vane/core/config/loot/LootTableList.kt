package org.oddlama.vane.core.config.loot

import org.oddlama.vane.core.config.ConfigDictSerializable
import java.util.Locale

class LootTableList : ConfigDictSerializable {
    private var tables: MutableList<LootDefinition?> = mutableListOf()

    fun tables(): MutableList<LootDefinition?> = tables

    // Map special internal names to PascalCase names for YAML generation
    override fun toDict(): MutableMap<String, Any> =
        tables.filterNotNull().associateTo(mutableMapOf()) { t -> toYamlName(t.name)!! to (t.serialize() as Any) }

    override fun fromDict(dict: MutableMap<String, Any>) {
        tables.clear()
        dict.entries.forEach { (name, raw) ->
            val internalName = if (name.isEmpty()) name else fromYamlName(name)
            tables.add(LootDefinition.deserialize(internalName, raw))
        }
    }

    companion object {
        @JvmStatic
        fun of(vararg defs: LootDefinition?): LootTableList =
            LootTableList().also { it.tables = defs.toMutableList() }

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
