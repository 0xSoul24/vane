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
import org.oddlama.vane.util.PlayerUtil

@VaneEnchantment(name = "rake", maxLevel = 4, rarity = Rarity.COMMON, treasure = true)
class Rake(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape(" H ", "HBH", " H ")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                .setIngredient('H', Material.GOLDEN_HOE)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        )
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerTillFarmland(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        // Only till additional blocks when right-clicking farmland
        if (event.clickedBlock!!.type != Material.FARMLAND) {
            return
        }

        // Get enchantment level
        val player = event.getPlayer()
        val item = player.equipment.getItem(event.hand!!)
        val level = item.getEnchantmentLevel(this.bukkit()!!)
        if (level == 0) {
            return
        }

        // Get tillable block
        val tillable = BlockUtil.nextTillableBlock(event.clickedBlock!!, level, true) ?: return

        // Till block
        if (PlayerUtil.tillBlock(player, tillable)) {
            ItemUtil.damageItem(player, item, 1)
            PlayerUtil.swingArm(player, event.hand!!)
        }
    }
}
