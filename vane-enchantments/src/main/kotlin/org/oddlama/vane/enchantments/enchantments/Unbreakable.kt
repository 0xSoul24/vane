package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.loot.LootTables
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.core.config.loot.LootDefinition
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments

@VaneEnchantment(name = "unbreakable", rarity = Rarity.RARE, treasure = true, allowCustom = true)
class Unbreakable(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("WAW", "NBN", "TST")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_the_gods")
                .setIngredient('W', Material.WITHER_ROSE)
                .setIngredient('A', Material.ENCHANTED_GOLDEN_APPLE)
                .setIngredient('N', Material.NETHERITE_INGOT)
                .setIngredient('T', Material.TOTEM_OF_UNDYING)
                .setIngredient('S', Material.NETHER_STAR)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    override fun defaultLootTables(): LootTableList {
        return LootTableList.of(
            LootDefinition("generic")
                .`in`(LootTables.ABANDONED_MINESHAFT)
                .add(1.0 / 120, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods")),
            LootDefinition("bastion")
                .`in`(LootTables.BASTION_TREASURE)
                .add(1.0 / 30, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onPlayerItemDamage(event: PlayerItemDamageEvent) {
        // Check enchantment
        val item = event.item
        if (item.getEnchantmentLevel(this.bukkit()!!) == 0) {
            return
        }

        // Set item unbreakable to prevent further event calls
        val meta = item.itemMeta
        meta.isUnbreakable = true
        // Also hide the internal unbreakable tag on the client
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        item.setItemMeta(meta)

        // Prevent damage
        event.damage = 0
        event.isCancelled = true
    }
}
