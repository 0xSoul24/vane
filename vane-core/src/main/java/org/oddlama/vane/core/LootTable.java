package org.oddlama.vane.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.functional.Consumer2;

public class LootTable {

    private Map<NamespacedKey, List<LootTableEntry>> possibleLoot = new HashMap<>();

    public LootTable() {}

    public LootTable put(final NamespacedKey key, final LootTableEntry entry) {
        possibleLoot.put(key, List.of(entry));
        return this;
    }

    public LootTable put(final NamespacedKey key, final List<LootTableEntry> entries) {
        possibleLoot.put(key, entries);
        return this;
    }

    public LootTable remove(final NamespacedKey key) {
        possibleLoot.remove(key);
        return this;
    }

    public Map<NamespacedKey, List<LootTableEntry>> possibleLoot() {
        return possibleLoot;
    }

    public List<LootTableEntry> flatCopy() {
        List<LootTableEntry> list = new ArrayList<>();
        possibleLoot.values().forEach(list::addAll);
        return list;
    }

    public void generateLoot(final List<ItemStack> output, final Random random) {
        for (final var set : possibleLoot.values()) {
            for (final var loot : set) {
                if (loot.evaluateChance(random)) {
                    loot.addSample(output, random);
                }
            }
        }
    }

    public ItemStack generateOverride(final Random random) {
        double totalChance = 0;
        final double threshold = random.nextDouble();
        final List<ItemStack> resultContainer = new ArrayList<>(1);
        final var lootList = flatCopy();
        Collections.shuffle(lootList, random);
        for (final var loot : lootList) {
            totalChance += loot.chance;
            if (totalChance > threshold) {
                loot.addSample(resultContainer, random);
            }
            if (!resultContainer.isEmpty()) {
                return resultContainer.get(0);
            }
        }
        return null;
    }

    public static class LootTableEntry {

        public double chance;
        public Consumer2<List<ItemStack>, Random> generator;

        public LootTableEntry(int rarityExpectedChests, final ItemStack item) {
            this(1.0 / rarityExpectedChests, item.clone(), 1, 1);
        }

        public LootTableEntry(int rarityExpectedChests, final ItemStack item, int amountMin, int amountMax) {
            this(1.0 / rarityExpectedChests, item.clone(), amountMin, amountMax);
        }

        public LootTableEntry(double chance, final ItemStack item, int amountMin, int amountMax) {
            this(chance, (list, random) -> {
                final var i = item.clone();
                final var amount = random.nextInt(amountMax - amountMin + 1) + amountMin;
                if (amount < 1) {
                    return;
                }

                i.setAmount(amount);
                list.add(i);
            });
        }

        public LootTableEntry(double chance, Consumer2<List<ItemStack>, Random> generator) {
            this.chance = chance;
            this.generator = generator;
        }

        public void addSample(final List<ItemStack> items, final Random random) {
            generator.apply(items, random);
        }

        public boolean evaluateChance(Random random) {
            return random.nextDouble() < chance;
        }
    }
}
