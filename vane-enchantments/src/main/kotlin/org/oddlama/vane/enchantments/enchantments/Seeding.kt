package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments
import org.oddlama.vane.util.BlockUtil
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.MaterialUtil
import org.oddlama.vane.util.PlayerUtil

@VaneEnchantment(name = "seeding", maxLevel = 4, rarity = Rarity.COMMON, treasure = true)
class Seeding(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("1 7", "2B6", "345")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                .setIngredient('1', Material.PUMPKIN_SEEDS)
                .setIngredient('2', Material.CARROT)
                .setIngredient('3', Material.WHEAT_SEEDS)
                .setIngredient('4', Material.NETHER_WART)
                .setIngredient('5', Material.BEETROOT_SEEDS)
                .setIngredient('6', Material.POTATO)
                .setIngredient('7', Material.MELON_SEEDS)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        )
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerRightClickPlant(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        // Only seed when right-clicking a plant
        val plantType = event.clickedBlock!!.type
        if (!MaterialUtil.isSeededPlant(plantType)) {
            return
        }

        // Get enchantment level
        val player = event.getPlayer()
        val item = player.equipment.getItem(event.hand!!)
        val level = item.getEnchantmentLevel(this.bukkit()!!)
        if (level == 0) {
            return
        }

        // Get seedable block
        val seedType = MaterialUtil.seedFor(plantType)
        val farmlandType = MaterialUtil.farmlandFor(seedType!!)
        val seedable = BlockUtil.nextSeedableBlock(event.clickedBlock!!, farmlandType, level) ?: return

        // Seed block
        if (PlayerUtil.seedBlock(player, item, seedable, plantType, seedType)) {
            ItemUtil.damageItem(player, item, 1)
            PlayerUtil.swingArm(player, event.hand!!)
        }
    }
}
