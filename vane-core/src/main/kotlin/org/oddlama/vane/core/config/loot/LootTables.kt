package org.oddlama.vane.core.config.loot

import org.bukkit.NamespacedKey
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigDict
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent

class LootTables<T : Module<T?>?> @JvmOverloads constructor(
    context: Context<T?>?,
    private val baseLootKey: NamespacedKey,
    private val defLoot: () -> LootTableList?,
    private val desc: String? = "The associated loot. This is a map of loot tables (as defined by minecraft) to additional loot. This additional loot is a list of loot definitions, which specify the amount and loot percentage for a particular item."
) : ModuleComponent<T?>(context) {
    @ConfigBoolean(def = true, desc = "Whether the loot should be registered. Set to false to quickly disable all associated loot.")
    var configRegisterLoot: Boolean = false

    @ConfigDict(cls = LootTableList::class, desc = "")
    private var configLoot: LootTableList? = null

    @Suppress("unused")
    fun configLootDef(): LootTableList? = defLoot()

    @Suppress("unused")
    fun configLootDesc(): String? = desc

    override fun onConfigChange() {
        configLoot!!.tables().filterNotNull().forEach { table ->
            table.affectedTables.forEach { tableKey ->
                module!!.lootTable(tableKey).remove(table.key(baseLootKey))
            }
        }
        if (enabled() && configRegisterLoot) {
            configLoot!!.tables().filterNotNull().forEach { table ->
                val entries = table.entries()
                table.affectedTables.forEach { tableKey ->
                    module!!.lootTable(tableKey).put(table.key(baseLootKey), entries.toMutableList())
                }
            }
        }
    }

    override fun onEnable() {}

    override fun onDisable() {}
}
