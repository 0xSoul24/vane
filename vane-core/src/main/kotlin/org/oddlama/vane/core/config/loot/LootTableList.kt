package org.oddlama.vane.core.config.loot

import org.oddlama.vane.core.config.ConfigDictSerializable
import java.util.*

class LootTableList : ConfigDictSerializable {
    private var tables: MutableList<LootDefinition?> = ArrayList<LootDefinition?>()

    fun tables(): MutableList<LootDefinition?> {
        return tables
    }

    // Map special internal names to PascalCase names for YAML generation
    override fun toDict(): MutableMap<String?, Any?> {
        val map: MutableMap<String?, Any?> = HashMap()
        for (t in tables) {
            if (t == null) continue
            val name = toYamlName(t.name())
            map[name] = t.serialize()
        }
        return map
    }

    override fun fromDict(dict: MutableMap<String?, Any?>?) {
        if (dict == null) return
        tables.clear()
        for (e in dict.entries) {
            val name = e.key
            val internalName = if (name.isNullOrEmpty()) name else fromYamlName(name)
            val raw = e.value ?: continue
            tables.add(LootDefinition.deserialize(internalName, raw))
        }
    }

    companion object {
        @JvmStatic
        fun of(vararg defs: LootDefinition?): LootTableList {
            val rl = LootTableList()
            rl.tables = defs.toMutableList()
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
                else -> s
            }
        }
    }
}
