package org.oddlama.vane.core

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Consumer2
import java.util.*

class LootTable {
    private val possibleLoot: MutableMap<NamespacedKey?, MutableList<LootTableEntry>> =
        HashMap<NamespacedKey?, MutableList<LootTableEntry>>()

    fun put(key: NamespacedKey?, entry: LootTableEntry): LootTable {
        possibleLoot[key] = mutableListOf(entry)
        return this
    }

    fun put(key: NamespacedKey?, entries: MutableList<LootTableEntry>?): LootTable {
        possibleLoot[key] = entries ?: ArrayList()
        return this
    }

    fun remove(key: NamespacedKey?): LootTable {
        possibleLoot.remove(key)
        return this
    }

    fun possibleLoot(): MutableMap<NamespacedKey?, MutableList<LootTableEntry>> {
        return possibleLoot
    }

    fun flatCopy(): MutableList<LootTableEntry> {
        val list: MutableList<LootTableEntry> = ArrayList<LootTableEntry>()
        possibleLoot.values.forEach { c -> list.addAll(c) }
        return list
    }

    fun generateLoot(output: MutableList<ItemStack?>?, random: Random) {
        for (set in possibleLoot.values) {
            for (loot in set) {
                if (loot.evaluateChance(random)) {
                    loot.addSample(output, random)
                }
            }
        }
    }

    fun generateOverride(random: Random): ItemStack? {
        var totalChance = 0.0
        val threshold = random.nextDouble()
        val resultContainer: MutableList<ItemStack?> = ArrayList<ItemStack?>(1)
        val lootList = flatCopy()
        lootList.shuffle(random)
        for (loot in lootList) {
            totalChance += loot.chance
            if (totalChance > threshold) {
                loot.addSample(resultContainer, random)
            }
            if (!resultContainer.isEmpty()) {
                return resultContainer[0]
            }
        }
        return null
    }

    class LootTableEntry(var chance: Double, var generator: Consumer2<MutableList<ItemStack?>?, Random?>) {
        constructor(rarityExpectedChests: Int, item: ItemStack) : this(1.0 / rarityExpectedChests, item.clone(), 1, 1)

        constructor(
            rarityExpectedChests: Int,
            item: ItemStack,
            amountMin: Int,
            amountMax: Int
        ) : this(1.0 / rarityExpectedChests, item.clone(), amountMin, amountMax)

        constructor(chance: Double, item: ItemStack, amountMin: Int, amountMax: Int) : this(
            chance,
            Consumer2 { list: MutableList<ItemStack?>?, random: Random? ->
                val i = item.clone()
                val amount = random!!.nextInt(amountMax - amountMin + 1) + amountMin
                if (amount >= 1) {
                    i.amount = amount
                    list!!.add(i)
                }
            })

        fun addSample(items: MutableList<ItemStack?>?, random: Random?) {
            generator.apply(items, random)
        }

        fun evaluateChance(random: Random): Boolean {
            return random.nextDouble() < chance
        }
    }
}
