package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.config.loot.LootDefinition;
import org.oddlama.vane.core.config.loot.LootTableList;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.enchantments.CustomEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "unbreakable", rarity = Rarity.RARE, treasure = true, allowCustom = true)
public class Unbreakable extends CustomEnchantment<Enchantments> {

    public Unbreakable(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("WAW", "NBN", "TST")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_the_gods")
                .setIngredient('W', Material.WITHER_ROSE)
                .setIngredient('A', Material.ENCHANTED_GOLDEN_APPLE)
                .setIngredient('N', Material.NETHERITE_INGOT)
                .setIngredient('T', Material.TOTEM_OF_UNDYING)
                .setIngredient('S', Material.NETHER_STAR)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        );
    }

    @Override
    public LootTableList defaultLootTables() {
        return LootTableList.of(
            new LootDefinition("generic")
                .in(LootTables.ABANDONED_MINESHAFT)
                .add(1.0 / 120, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods")),
            new LootDefinition("bastion")
                .in(LootTables.BASTION_TREASURE)
                .add(1.0 / 30, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        );
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerItemDamage(final PlayerItemDamageEvent event) {
        // Check enchantment
        final var item = event.getItem();
        if (item.getEnchantmentLevel(this.bukkit()) == 0) {
            return;
        }

        // Set item unbreakable to prevent further event calls
        final var meta = item.getItemMeta();
        meta.setUnbreakable(true);
        // Also hide the internal unbreakable tag on the client
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);

        // Prevent damage
        event.setDamage(0);
        event.setCancelled(true);
    }
}
