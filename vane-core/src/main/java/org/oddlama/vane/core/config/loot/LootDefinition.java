package org.oddlama.vane.core.config.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.core.LootTable.LootTableEntry;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.StorageUtil;

public class LootDefinition {

    private record Entry(double chance, int amountMin, int amountMax, String itemDefinition) {
        public Object serialize() {
            final HashMap<String, Object> dict = new HashMap<>();
            dict.put("Chance", chance);
            dict.put("AmountMin", amountMin);
            dict.put("AmountMax", amountMax);
            dict.put("Item", itemDefinition);
            return dict;
        }

        public static Entry deserialize(Map<String, Object> map) {
            if (!(map.get("Chance") instanceof Double chance)) {
                throw new IllegalArgumentException("Invalid loot table entry: chance must be a double!");
            }

            if (!(map.get("AmountMin") instanceof Integer amountMin)) {
                throw new IllegalArgumentException("Invalid loot table entry: amountMin must be a int!");
            }

            if (!(map.get("AmountMax") instanceof Integer amountMax)) {
                throw new IllegalArgumentException("Invalid loot table entry: amountMax must be a int!");
            }

            if (!(map.get("Item") instanceof String itemDefinition)) {
                throw new IllegalArgumentException("Invalid loot table entry: itemDefinition must be a String!");
            }

            return new Entry(chance, amountMin, amountMax, itemDefinition);
        }
    }

    public String name;
    public List<NamespacedKey> affectedTables = new ArrayList<>();
    private List<Entry> entries = new ArrayList<>();

    public LootDefinition(final String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public NamespacedKey key(final NamespacedKey baseKey) {
        return StorageUtil.namespacedKey(baseKey.namespace(), baseKey.value() + "." + name);
    }

    public LootDefinition in(final NamespacedKey table) {
        affectedTables.add(table);
        return this;
    }

    public LootDefinition in(final LootTables table) {
        return in(table.getKey());
    }

    private LootDefinition add(Entry entry) {
        entries.add(entry);
        return this;
    }

    public LootDefinition add(double chance, int amountMin, int amountMax, String itemDefinition) {
        return add(new Entry(chance, amountMin, amountMax, itemDefinition));
    }

    public Object serialize() {
        final HashMap<String, Object> dict = new HashMap<>();
        // Use capitalized keys for YAML generation: "Tables" and "Items"
        dict.put("Tables", affectedTables.stream().map(NamespacedKey::toString).toList());
        dict.put("Items", entries.stream().map(Entry::serialize).toList());
        return dict;
    }

    @SuppressWarnings("unchecked")
    public static LootDefinition deserialize(String name, Object rawDict) {
        if (!(rawDict instanceof Map<?, ?> dict)) {
            throw new IllegalArgumentException(
                "Invalid loot table: Argument must be a Map<String, Object>, but is " + rawDict.getClass() + "!"
            );
        }

        final var tableDict = (Map<String, Object>) dict;
        final Object tablesObj = tableDict.containsKey("Tables") ? tableDict.get("Tables") : tableDict.get("tables");
        if (!(tablesObj instanceof List<?> tables)) {
            throw new IllegalArgumentException(
                "Invalid loot table: 'tables' must be a list"
            );
        }

        final Object itemsObj = tableDict.containsKey("Items") ? tableDict.get("Items") : tableDict.get("items");
        if (!(itemsObj instanceof List<?> items)) {
            throw new IllegalArgumentException(
                "Invalid loot table: 'items' must be a list"
            );
        }

        final var table = new LootDefinition(name);
        for (final var e : tables) {
            if (e instanceof String key) {
                table.in(NamespacedKey.fromString(key));
            }
        }
        for (final var e : items) {
            if (e instanceof Map<?, ?> map) {
                table.add(Entry.deserialize((Map<String, Object>) map));
            }
        }

        return table;
    }

    public List<LootTableEntry> entries() {
        return entries
            .stream()
            .map(e ->
                new LootTableEntry(
                    e.chance,
                    ItemUtil.itemstackFromString(e.itemDefinition).getLeft(),
                    e.amountMin,
                    e.amountMax
                )
            )
            .toList();
    }
}
