package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.loot.LootTables
import org.oddlama.vane.annotation.config.ConfigDoubleList
import org.oddlama.vane.annotation.config.ConfigIntList
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.core.config.loot.LootDefinition
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments
import org.oddlama.vane.util.msToTicks
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.PlayerUtil

@VaneEnchantment(name = "wings", maxLevel = 4, rarity = Rarity.RARE, treasure = true, allowCustom = true)
class Wings(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    @ConfigIntList(
        def = [7000, 5000, 3500, 2800],
        min = 0,
        desc = "Boost cooldown in milliseconds for each enchantment level."
    )
    private val configBoostCooldowns: MutableList<Int?>? = null

    @ConfigDoubleList(def = [0.4, 0.47, 0.54, 0.6], min = 0.0, desc = "Boost strength for each enchantment level.")
    private val configBoostStrengths: MutableList<Double?>? = null

    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("M M", "DBD", "R R")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                .setIngredient('M', Material.PHANTOM_MEMBRANE)
                .setIngredient('D', Material.DISPENSER)
                .setIngredient('R', Material.FIREWORK_ROCKET)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
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
                .add(1.0 / 110, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_knowledge")),
            LootDefinition("bastion")
                .`in`(LootTables.BASTION_TREASURE)
                .add(1.0 / 10, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        )
    }

    private fun getBoostCooldown(level: Int): Int {
        if (level > 0 && level <= configBoostCooldowns!!.size) {
            return configBoostCooldowns[level - 1]!!
        }
        return configBoostCooldowns!![0]!!
    }

    private fun getBoostStrength(level: Int): Double {
        if (level > 0 && level <= configBoostStrengths!!.size) {
            return configBoostStrengths[level - 1]!!
        }
        return configBoostStrengths!![0]!!
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        // Check sneaking and flying
        val player = event.getPlayer()
        if (!event.isSneaking || !player.isGliding) {
            return
        }

        // Check enchantment level
        val chest = player.equipment.chestplate
        val level = chest.getEnchantmentLevel(this.bukkit()!!)
        if (level == 0) {
            return
        }

        // Check cooldown
        if (player.getCooldown(Material.ELYTRA) > 0) {
            return
        }

        // Apply boost
        val cooldown = msToTicks(getBoostCooldown(level).toLong())
        player.setCooldown(Material.ELYTRA, cooldown.toInt())
        PlayerUtil.applyElytraBoost(player, getBoostStrength(level))
        ItemUtil.damageItem(player, chest, (1.0 + 2.0 * Math.random()).toInt())

        // Spawn particles
        PlayerUtil.spawnElytraBoostParticles(player)
    }
}
