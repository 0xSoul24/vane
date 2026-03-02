package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.loot.LootTables
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.annotation.config.ConfigDoubleList
import org.oddlama.vane.core.config.loot.LootDefinition
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.PlayerUtil
import org.bukkit.entity.Player

@VaneEnchantment(name = "take_off", maxLevel = 3, rarity = Rarity.UNCOMMON, treasure = true, allowCustom = true)
class TakeOff(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    @ConfigDoubleList(def = [0.2, 0.4, 0.6], min = 0.0, desc = "Boost strength for each enchantment level.")
    private val configBoostStrengths: MutableList<Double?>? = null

    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("MBM", "PSP")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_the_gods")
                .setIngredient('M', Material.PHANTOM_MEMBRANE)
                .setIngredient('P', Material.PISTON)
                .setIngredient('S', Material.SLIME_BLOCK)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    override fun defaultLootTables(): LootTableList {
        return LootTableList.of(
            LootDefinition("generic")
                .`in`(LootTables.BURIED_TREASURE)
                .`in`(LootTables.PILLAGER_OUTPOST)
                .`in`(LootTables.RUINED_PORTAL)
                .`in`(LootTables.SHIPWRECK_TREASURE)
                .`in`(LootTables.STRONGHOLD_LIBRARY)
                .`in`(LootTables.UNDERWATER_RUIN_BIG)
                .`in`(LootTables.UNDERWATER_RUIN_SMALL)
                .`in`(LootTables.VILLAGE_TEMPLE)
                .`in`(LootTables.WOODLAND_MANSION)
                .add(1.0 / 150, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    private fun getBoostStrength(level: Int): Double {
        if (level > 0 && level <= configBoostStrengths!!.size) {
            return configBoostStrengths[level - 1]!!
        }
        return configBoostStrengths!![0]!!
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerToggleGlide(event: EntityToggleGlideEvent) {
        if (event.getEntity() !is Player || !event.isGliding) {
            return
        }

        // Don't apply for sneaking players
        val player = event.getEntity() as Player
        if (player.isSneaking) {
            return
        }

        // Check enchantment level
        val chest = player.equipment.chestplate
        val level = chest.getEnchantmentLevel(this.bukkit()!!)
        if (level == 0) {
            return
        }

        // Apply boost
        PlayerUtil.applyElytraBoost(player, getBoostStrength(level))
        ItemUtil.damageItem(player, chest, (1.0 + 2.0 * Math.random()).toInt())

        // Spawn particles
        PlayerUtil.spawnElytraBoostParticles(player)
    }
}
