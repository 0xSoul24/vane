package org.oddlama.vane.enchantments.enchantments;

import static org.oddlama.vane.util.BlockUtil.nextSeedableBlock;
import static org.oddlama.vane.util.ItemUtil.damageItem;
import static org.oddlama.vane.util.MaterialUtil.farmlandFor;
import static org.oddlama.vane.util.MaterialUtil.isSeededPlant;
import static org.oddlama.vane.util.MaterialUtil.seedFor;
import static org.oddlama.vane.util.PlayerUtil.seedBlock;
import static org.oddlama.vane.util.PlayerUtil.swingArm;

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

@VaneEnchantment(
    name = "seeding",
    maxLevel = 4,
    rarity = Rarity.COMMON,
    treasure = true
)
public class Seeding extends CustomEnchantment<Enchantments> {

    public Seeding(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
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
        );
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerRightClickPlant(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Only seed when right-clicking a plant
        final var plantType = event.getClickedBlock().getType();
        if (!isSeededPlant(plantType)) {
            return;
        }

        // Get enchantment level
        final var player = event.getPlayer();
        final var item = player.getEquipment().getItem(event.getHand());
        final var level = item.getEnchantmentLevel(this.bukkit());
        if (level == 0) {
            return;
        }

        // Get seedable block
        final var seedType = seedFor(plantType);
        final var farmlandType = farmlandFor(seedType);
        final var seedable = nextSeedableBlock(event.getClickedBlock(), farmlandType, level);
        if (seedable == null) {
            return;
        }

        // Seed block
        if (seedBlock(player, item, seedable, plantType, seedType)) {
            damageItem(player, item, 1);
            swingArm(player, event.getHand());
        }
    }
}
