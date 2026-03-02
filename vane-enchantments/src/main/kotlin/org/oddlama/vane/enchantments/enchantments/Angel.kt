package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.loot.LootTables
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.config.ConfigDoubleList
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.core.config.loot.LootDefinition
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments

@VaneEnchantment(name = "angel", maxLevel = 5, rarity = Rarity.VERY_RARE, treasure = true, allowCustom = true)
class Angel(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    @ConfigDouble(
        def = 0.1,
        min = 0.0,
        max = 1.0,
        desc = "Acceleration percentage. Each tick, the current flying speed is increased X percent towards the target speed. Low values (~0.1) typically result in a smooth acceleration curve and a natural feeling."
    )
    private val configAccelerationPercentage = 0.0

    @ConfigDoubleList(
        def = [0.7, 1.1, 1.4, 1.7, 2.0],
        min = 0.0,
        desc = "Flying speed in blocks per second for each enchantment level."
    )
    private val configSpeed: MutableList<Double?>? = null

    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("PRP", "MBM", "MDM")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_the_gods")
                .setIngredient('M', Material.PHANTOM_MEMBRANE)
                .setIngredient('D', Material.DRAGON_BREATH)
                .setIngredient('P', Material.PUFFERFISH_BUCKET)
                .setIngredient('R', Material.FIREWORK_ROCKET)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    override fun defaultLootTables(): LootTableList {
        return LootTableList.of(
            LootDefinition("generic")
                .`in`(LootTables.BURIED_TREASURE)
                .`in`(LootTables.PILLAGER_OUTPOST)
                .`in`(LootTables.RUINED_PORTAL)
                .`in`(LootTables.STRONGHOLD_LIBRARY)
                .`in`(LootTables.UNDERWATER_RUIN_BIG)
                .`in`(LootTables.VILLAGE_TEMPLE)
                .add(1.0 / 250, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    private fun getSpeed(level: Int): Double {
        if (level > 0 && level <= configSpeed!!.size) {
            return configSpeed[level - 1]!!
        }
        return configSpeed!![0]!!
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        // Check sneaking and flying
        val player = event.getPlayer()
        if (!player.isSneaking || !player.isGliding) {
            return
        }

        // Check enchantment level
        val chest = player.equipment.chestplate ?: // Can happen due to other plugins
        return
        val level = chest.getEnchantmentLevel(this.bukkit()!!)
        if (level == 0) {
            return
        }

        val loc = player.location
        val dir = loc.getDirection()
        if (dir.length() == 0.0) {
            return
        }

        // Scale the delta dependent on the angle. Higher angle -> less effect
        val vel = player.velocity
        val delta = configAccelerationPercentage * (1.0 - dir.angle(vel) / Math.PI)
        val factor = getSpeed(level)

        // Exponential moving average between velocity and target velocity
        val newVel = vel.multiply(1.0 - delta).add(dir.normalize().multiply(delta * factor))
        player.velocity = newVel

        // Spawn particles
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 0, -newVel.getX(), -newVel.getY(), -newVel.getZ(), 0.4)
    }
}
