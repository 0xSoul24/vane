package org.oddlama.vane.core.config.loot

import org.bukkit.NamespacedKey
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigDict
import org.oddlama.vane.core.LootTable
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent
import java.util.function.Consumer
import java.util.function.Supplier

class LootTables<T : Module<T?>?> @JvmOverloads constructor(
    context: Context<T?>?,
    private val baseLootKey: NamespacedKey,
    private val defLoot: Supplier<LootTableList?>,
    private val desc: String? = "The associated loot. This is a map of loot tables (as defined by minecraft) to additional loot. This additional loot is a list of loot definitions, which specify the amount and loot percentage for a particular item."
) : ModuleComponent<T?>(context) {
    @ConfigBoolean(
        def = true,
        desc = "Whether the loot should be registered. Set to false to quickly disable all associated loot."
    )
    var configRegisterLoot: Boolean = false

    @ConfigDict(cls = LootTableList::class, desc = "")
    private var configLoot: LootTableList? = null

    @Suppress("unused")
    fun configLootDef(): LootTableList? {
        return defLoot.get()
    }

    @Suppress("unused")
    fun configLootDesc(): String? {
        return desc
    }

    override fun onConfigChange() {
        // Loot tables are processed in onConfigChange and not in onModuleDisable() / onModuleEnable(),
        // as the current loot table modifications need to be removed even if we are disabled
        // afterward.
        configLoot!!
            .tables()
            .forEach(Consumer { table: LootDefinition? ->
                table!!.affectedTables.forEach(Consumer { tableKey: NamespacedKey? ->
                    module!!.lootTable(tableKey).remove(table.key(baseLootKey))
                }
                )
            }
            )
        if (enabled() && configRegisterLoot) {
            configLoot!!
                .tables()
                .forEach(Consumer { table: LootDefinition? ->
                    val entriesNullable = table!!.entries()
                    val entries: MutableList<LootTable.LootTableEntry> = ArrayList()
                    for (e in entriesNullable) {
                        if (e != null) entries.add(e)
                    }
                    table.affectedTables.forEach(Consumer { tableKey: NamespacedKey? ->
                        module!!.lootTable(tableKey).put(table.key(baseLootKey), entries)
                    }
                    )
                })
        }
    }

    override fun onEnable() {}

    override fun onDisable() {}
}
