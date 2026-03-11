package org.oddlama.vane.core

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Consumer2
import java.util.*

class LootTable {
    private val possibleLoot = mutableMapOf<NamespacedKey, MutableList<LootTableEntry>>()

    fun put(key: NamespacedKey, entry: LootTableEntry): LootTable = apply {
        possibleLoot[key] = mutableListOf(entry)
    }

    fun put(key: NamespacedKey, entries: MutableList<LootTableEntry>?): LootTable = apply {
        possibleLoot[key] = entries ?: mutableListOf()
    }

    fun remove(key: NamespacedKey): LootTable = apply { possibleLoot.remove(key) }

    fun possibleLoot(): Map<NamespacedKey, List<LootTableEntry>> = possibleLoot

    fun flatCopy(): MutableList<LootTableEntry> =
        possibleLoot.values.flatMapTo(mutableListOf()) { it }

    fun generateLoot(output: MutableList<ItemStack?>?, random: Random) {
        possibleLoot.values.forEach { set ->
            set.forEach { loot ->
                if (loot.evaluateChance(random)) loot.addSample(output, random)
            }
        }
    }

    fun generateOverride(random: Random): ItemStack? {
        var totalChance = 0.0
        val threshold = random.nextDouble()
        val resultContainer = mutableListOf<ItemStack?>()
        flatCopy().shuffled(random).forEach { loot ->
            totalChance += loot.chance
            if (totalChance > threshold) loot.addSample(resultContainer, random)
            if (resultContainer.isNotEmpty()) return resultContainer[0]
        }
        return null
    }

    class LootTableEntry(val chance: Double, val generator: Consumer2<MutableList<ItemStack?>?, Random?>) {
        constructor(rarityExpectedChests: Int, item: ItemStack) :
            this(1.0 / rarityExpectedChests, item.clone(), 1, 1)

        constructor(rarityExpectedChests: Int, item: ItemStack, amountMin: Int, amountMax: Int) :
            this(1.0 / rarityExpectedChests, item.clone(), amountMin, amountMax)

        constructor(chance: Double, item: ItemStack, amountMin: Int, amountMax: Int) : this(
            chance,
            Consumer2 { list, random ->
                val amount = random!!.nextInt(amountMax - amountMin + 1) + amountMin
                if (amount >= 1) list!!.add(item.clone().also { it.amount = amount })
            }
        )

        fun addSample(items: MutableList<ItemStack?>?, random: Random?) = generator.apply(items, random)
        fun evaluateChance(random: Random): Boolean = random.nextDouble() < chance
    }
}
