package org.oddlama.vane.core.config.loot;

import java.util.function.Supplier;
import org.bukkit.NamespacedKey;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigDict;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

public class LootTables<T extends Module<T>> extends ModuleComponent<T> {

    private final NamespacedKey baseLootKey;

    @ConfigBoolean(
        def = true,
        desc = "Whether the loot should be registered. Set to false to quickly disable all associated loot."
    )
    public boolean configRegisterLoot;

    @ConfigDict(cls = LootTableList.class, desc = "")
    private LootTableList configLoot;

    private Supplier<LootTableList> defLoot;
    private String desc;

    public LootTables(
        final Context<T> context,
        final NamespacedKey baseLootKey,
        final Supplier<LootTableList> defLoot
    ) {
        this(
            context,
                baseLootKey,
                defLoot,
            "The associated loot. This is a map of loot tables (as defined by minecraft) to additional loot. This additional loot is a list of loot definitions, which specify the amount and loot percentage for a particular item."
        );
    }

    public LootTables(
        final Context<T> context,
        final NamespacedKey baseLootKey,
        final Supplier<LootTableList> defLoot,
        final String desc
    ) {
        super(context);
        this.baseLootKey = baseLootKey;
        this.defLoot = defLoot;
        this.desc = desc;
    }

    public LootTableList configLootDef() {
        return defLoot.get();
    }

    public String configLootDesc() {
        return desc;
    }

    @Override
    public void onConfigChange() {
        // Loot tables are processed in onConfigChange and not in onModuleDisable() / onModuleEnable(),
        // as the current loot table modifications need to be removed even if we are disabled
        // afterward.
        configLoot
            .tables()
            .forEach(table ->
                table.affectedTables.forEach(tableKey ->
                    getModule().lootTable(tableKey).remove(table.key(baseLootKey))
                )
            );
        if (enabled() && configRegisterLoot) {
            configLoot
                .tables()
                .forEach(table -> {
                    final var entries = table.entries();
                    table.affectedTables.forEach(tableKey ->
                        getModule().lootTable(tableKey).put(table.key(baseLootKey), entries)
                    );
                });
        }
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}
}
