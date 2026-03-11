package org.oddlama.vane.core.config.loot

import org.bukkit.NamespacedKey
import org.bukkit.loot.LootTables
import org.oddlama.vane.core.LootTable
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.StorageUtil.namespacedKey

class LootDefinition(val name: String?) {
    private data class Entry(
        val chance: Double,
        val amountMin: Int,
        val amountMax: Int,
        val itemDefinition: String?
    ) {
        fun serialize(): Map<String, Any?> = mapOf(
            "Chance" to chance,
            "AmountMin" to amountMin,
            "AmountMax" to amountMax,
            "Item" to itemDefinition
        )

        companion object {
            fun deserialize(map: Map<String?, Any?>): Entry {
                val chance = when (val v = map["Chance"]) {
                    is Double -> v
                    is Float  -> v.toDouble()
                    is Int    -> v.toDouble()
                    is Long   -> v.toDouble()
                    else -> throw IllegalArgumentException("Invalid loot table entry: chance must be a number (double)!")
                }
                val amountMin = when (val v = map["AmountMin"]) {
                    is Int    -> v
                    is Double -> v.toInt()
                    is Long   -> v.toInt()
                    else -> throw IllegalArgumentException("Invalid loot table entry: amountMin must be an int!")
                }
                val amountMax = when (val v = map["AmountMax"]) {
                    is Int    -> v
                    is Double -> v.toInt()
                    is Long   -> v.toInt()
                    else -> throw IllegalArgumentException("Invalid loot table entry: amountMax must be an int!")
                }
                val itemDefinition = map["Item"]?.toString()
                return Entry(chance, amountMin, amountMax, itemDefinition)
            }
        }
    }

    val affectedTables: MutableList<NamespacedKey?> = mutableListOf()
    private val entries: MutableList<Entry> = mutableListOf()

    fun key(baseKey: NamespacedKey): NamespacedKey =
        namespacedKey(baseKey.namespace(), "${baseKey.value()}.$name")

    fun `in`(table: NamespacedKey?): LootDefinition = apply { affectedTables.add(table) }
    fun `in`(table: LootTables): LootDefinition = `in`(table.key)

    private fun add(entry: Entry): LootDefinition = apply { entries.add(entry) }

    fun add(chance: Double, amountMin: Int, amountMax: Int, itemDefinition: String?): LootDefinition =
        add(Entry(chance, amountMin, amountMax, itemDefinition))

    fun serialize(): Map<String, Any> = mapOf(
        "Tables" to affectedTables.map { it.toString() },
        "Items"  to entries.map { it.serialize() }
    )

    fun entries(): List<LootTable.LootTableEntry> = entries.map { e ->
        LootTable.LootTableEntry(
            e.chance,
            ItemUtil.itemstackFromString(e.itemDefinition!!).getLeft()!!,
            e.amountMin,
            e.amountMax
        )
    }

    companion object {
        fun deserialize(name: String?, rawDict: Any): LootDefinition {
            require(rawDict is Map<*, *>) {
                "Invalid loot table: Argument must be a Map<String, Object>, but is ${rawDict.javaClass}!"
            }

            val tableDict = rawDict.entries
                .filter { it.key is String }
                .associate { it.key as String to it.value }

            val tablesObj = tableDict["Tables"] ?: tableDict["tables"]
            require(tablesObj is List<*>) { "Invalid loot table: 'tables' must be a list" }

            val itemsObj = tableDict["Items"] ?: tableDict["items"]
            require(itemsObj is List<*>) { "Invalid loot table: 'items' must be a list" }

            return LootDefinition(name).also { table ->
                tablesObj.filterIsInstance<String>().forEach { table.`in`(NamespacedKey.fromString(it)) }
                itemsObj.filterIsInstance<Map<*, *>>().forEach { e ->
                    val mapped = e.entries
                        .filter { it.key is String }
                        .associate { it.key as String? to it.value }
                    table.add(Entry.deserialize(mapped))
                }
            }
        }
    }
}
