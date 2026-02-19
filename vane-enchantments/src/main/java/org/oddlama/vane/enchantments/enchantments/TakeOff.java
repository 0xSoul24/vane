package org.oddlama.vane.enchantments.enchantments;

import static org.oddlama.vane.util.ItemUtil.damageItem;
import static org.oddlama.vane.util.PlayerUtil.applyElytraBoost;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.loot.LootTables;
import org.bukkit.util.Vector;
import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.config.loot.LootDefinition;
import org.oddlama.vane.core.config.loot.LootTableList;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.enchantments.CustomEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "take_off", maxLevel = 3, rarity = Rarity.UNCOMMON, treasure = true, allowCustom = true)
public class TakeOff extends CustomEnchantment<Enchantments> {

    @ConfigDoubleList(def = { 0.2, 0.4, 0.6 }, min = 0.0, desc = "Boost strength for each enchantment level.")
    private List<Double> configBoostStrengths;

    public TakeOff(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("MBM", "PSP")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_the_gods")
                .setIngredient('M', Material.PHANTOM_MEMBRANE)
                .setIngredient('P', Material.PISTON)
                .setIngredient('S', Material.SLIME_BLOCK)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        );
    }

    @Override
    public LootTableList defaultLootTables() {
        return LootTableList.of(
            new LootDefinition("generic")
                .in(LootTables.BURIED_TREASURE)
                .in(LootTables.PILLAGER_OUTPOST)
                .in(LootTables.RUINED_PORTAL)
                .in(LootTables.SHIPWRECK_TREASURE)
                .in(LootTables.STRONGHOLD_LIBRARY)
                .in(LootTables.UNDERWATER_RUIN_BIG)
                .in(LootTables.UNDERWATER_RUIN_SMALL)
                .in(LootTables.VILLAGE_TEMPLE)
                .in(LootTables.WOODLAND_MANSION)
                .add(1.0 / 150, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        );
    }

    private double getBoostStrength(int level) {
        if (level > 0 && level <= configBoostStrengths.size()) {
            return configBoostStrengths.get(level - 1);
        }
        return configBoostStrengths.get(0);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player) || !event.isGliding()) {
            return;
        }

        // Don't apply for sneaking players
        final var player = (Player) event.getEntity();
        if (player.isSneaking()) {
            return;
        }

        // Check enchantment level
        final var chest = player.getEquipment().getChestplate();
        final var level = chest.getEnchantmentLevel(this.bukkit());
        if (level == 0) {
            return;
        }

        // Apply boost
        applyElytraBoost(player, getBoostStrength(level));
        damageItem(player, chest, (int) (1.0 + 2.0 * Math.random()));

        // Spawn particles
        final var loc = player.getLocation();
        final var vel = player.getVelocity().length();
        for (int i = 0; i < 16; ++i) {
            final var rnd = Vector.getRandom().subtract(new Vector(.5, .5, .5)).normalize().multiply(.25);
            final var dir = rnd.clone().multiply(.5).subtract(player.getVelocity());
            loc
                .getWorld()
                .spawnParticle(
                    Particle.FIREWORK,
                    loc.add(rnd),
                    0,
                    dir.getX(),
                    dir.getY(),
                    dir.getZ(),
                    vel * ThreadLocalRandom.current().nextDouble(0.4, 0.6)
                );
        }
    }
}
