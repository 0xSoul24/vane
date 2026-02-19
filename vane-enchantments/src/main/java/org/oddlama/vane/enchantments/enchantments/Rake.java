package org.oddlama.vane.enchantments.enchantments;

import static org.oddlama.vane.util.BlockUtil.nextTillableBlock;
import static org.oddlama.vane.util.ItemUtil.damageItem;
import static org.oddlama.vane.util.PlayerUtil.swingArm;
import static org.oddlama.vane.util.PlayerUtil.tillBlock;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.enchantments.CustomEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "rake", maxLevel = 4, rarity = Rarity.COMMON, treasure = true)
public class Rake extends CustomEnchantment<Enchantments> {

    public Rake(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape(" H ", "HBH", " H ")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                .setIngredient('H', Material.GOLDEN_HOE)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        );
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerTillFarmland(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Only till additional blocks when right-clicking farmland
        if (event.getClickedBlock().getType() != Material.FARMLAND) {
            return;
        }

        // Get enchantment level
        final var player = event.getPlayer();
        final var item = player.getEquipment().getItem(event.getHand());
        final var level = item.getEnchantmentLevel(this.bukkit());
        if (level == 0) {
            return;
        }

        // Get tillable block
        final var tillable = nextTillableBlock(event.getClickedBlock(), level, true);
        if (tillable == null) {
            return;
        }

        // Till block
        if (tillBlock(player, tillable)) {
            damageItem(player, item, 1);
            swingArm(player, event.getHand());
        }
    }
}
