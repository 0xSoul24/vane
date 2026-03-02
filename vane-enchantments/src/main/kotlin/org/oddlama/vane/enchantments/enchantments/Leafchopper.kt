package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.data.type.Leaves
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments

@VaneEnchantment(name = "leafchopper", rarity = Rarity.COMMON, treasure = true)
class Leafchopper(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape(" S ", "SBS", " S ")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                .setIngredient('S', Material.SHEARS)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerLeftClickLeaves(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.hand != EquipmentSlot.HAND || event.action != Action.LEFT_CLICK_BLOCK
        ) {
            return
        }

        // Check leaves
        val block = event.clickedBlock
        val data = block!!.blockData
        if (data !is Leaves) {
            return
        }

        // Check non-persistent leaves
        val leaves = data
        if (leaves.isPersistent) {
            return
        }

        // Check enchantment level
        val player = event.getPlayer()
        val item = player.equipment.itemInMainHand
        val level = item.getEnchantmentLevel(this.bukkit()!!)
        if (level == 0) {
            return
        }

        // Break instantly, for no additional durability cost.
        block.breakNaturally()
        block.world.playSound(block.location, Sound.BLOCK_GRASS_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f)
    }
}
