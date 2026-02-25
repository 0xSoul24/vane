package org.oddlama.vane.core.config.loot

import org.bukkit.NamespacedKey
import org.bukkit.loot.LootTables
import org.oddlama.vane.core.LootTable
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.StorageUtil.namespacedKey

class LootDefinition(var name: String?) {
    @JvmRecord
    private data class Entry(val chance: Double, val amountMin: Int, val amountMax: Int, val itemDefinition: String?) {
        fun serialize(): Any {
            val dict = HashMap<String?, Any?>()
            dict["Chance"] = chance
            dict["AmountMin"] = amountMin
            dict["AmountMax"] = amountMax
            dict["Item"] = itemDefinition
            return dict
        }

        companion object {
            fun deserialize(map: MutableMap<String?, Any?>): Entry {
                // Accept a few numeric representations and convert them to the expected types
                val chance = when (val v = map["Chance"]) {
                    is Double -> v
                    is Float -> v.toDouble()
                    is Int -> v.toDouble()
                    is Long -> v.toDouble()
                    else -> throw IllegalArgumentException("Invalid loot table entry: chance must be a number (double)!")
                }

                val amountMin = when (val v = map["AmountMin"]) {
                    is Int -> v
                    is Double -> v.toInt()
                    is Long -> v.toInt()
                    else -> throw IllegalArgumentException("Invalid loot table entry: amountMin must be an int!")
                }

                val amountMax = when (val v = map["AmountMax"]) {
                    is Int -> v
                    is Double -> v.toInt()
                    is Long -> v.toInt()
                    else -> throw IllegalArgumentException("Invalid loot table entry: amountMax must be an int!")
                }

                val itemDefinition = when (val v = map["Item"]) {
                    is String -> v
                    null -> null
                    else -> v.toString()
                }

                return Entry(chance, amountMin, amountMax, itemDefinition)
            }
        }
    }

    var affectedTables: MutableList<NamespacedKey?> = ArrayList()
    private val entries: MutableList<Entry?> = ArrayList<Entry?>()

    fun name(): String? {
        return name
    }

    fun key(baseKey: NamespacedKey): NamespacedKey {
        return namespacedKey(baseKey.namespace(), baseKey.value() + "." + name)
    }

    fun `in`(table: NamespacedKey?): LootDefinition {
        affectedTables.add(table)
        return this
    }

    fun `in`(table: LootTables): LootDefinition {
        return `in`(table.key)
    }

    private fun add(entry: Entry?): LootDefinition {
        entries.add(entry)
        return this
    }

    fun add(chance: Double, amountMin: Int, amountMax: Int, itemDefinition: String?): LootDefinition {
        return add(Entry(chance, amountMin, amountMax, itemDefinition))
    }

    fun serialize(): Any {
        val dict = HashMap<String?, Any?>()
        // Use capitalized keys for YAML generation: "Tables" and "Items"
        dict["Tables"] = affectedTables.stream().map { obj: NamespacedKey? -> obj.toString() }.toList()
        dict["Items"] = entries.stream().map { obj: Entry? -> obj!!.serialize() }.toList()
        return dict
    }

    fun entries(): MutableList<LootTable.LootTableEntry?> {
        return entries
            .stream()
            .map { e: Entry? ->
                LootTable.LootTableEntry(
                    e!!.chance,
                    ItemUtil.itemstackFromString(e.itemDefinition!!).getLeft()!!,
                    e.amountMin,
                    e.amountMax
                )
            }
            .toList()
            .toMutableList()
    }

    companion object {
        fun deserialize(name: String?, rawDict: Any): LootDefinition {
            require(rawDict is MutableMap<*, *>) { "Invalid loot table: Argument must be a Map<String, Object>, but is " + rawDict.javaClass + "!" }

            // Safely build a typed map from the raw (untyped) map
            val tableDict = HashMap<String?, Any?>()
            for ((k, v) in rawDict) {
                if (k is String) {
                    tableDict[k] = v
                }
            }

            val tablesObj = if (tableDict.containsKey("Tables")) tableDict["Tables"] else tableDict["tables"]
            require(tablesObj is MutableList<*>) { "Invalid loot table: 'tables' must be a list" }

            val itemsObj = if (tableDict.containsKey("Items")) tableDict["Items"] else tableDict["items"]
            require(itemsObj is MutableList<*>) { "Invalid loot table: 'items' must be a list" }

            val table = LootDefinition(name)
            for (e in tablesObj) {
                if (e is String) {
                    table.`in`(NamespacedKey.fromString(e))
                }
            }
            for (e in itemsObj) {
                if (e is MutableMap<*, *>) {
                    val mapped = HashMap<String?, Any?>()
                    for ((mk, mv) in e) {
                        if (mk is String) mapped[mk] = mv
                    }
                    table.add(Entry.deserialize(mapped))
                }
            }

            return table
        }
    }
}
