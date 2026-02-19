package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.enchantments.CustomEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;

import java.util.List;

@VaneEnchantment(
    name = "grappling_hook",
    maxLevel = 3,
    rarity = Rarity.UNCOMMON,
    treasure = true
)
public class GrapplingHook extends CustomEnchantment<Enchantments> {

    // Constant offset to the added velocity, so the player will always move up a little.
    private static final Vector CONSTANT_OFFSET = new Vector(0.0, 0.2, 0.0);
    private static final double BOUNDING_BOX_RADIUS = 0.2;

    @ConfigDouble(
        def = 16.0,
        min = 2.0,
        max = 50.0,
        desc = "Ideal grappling distance for maximum grapple strength. Strength increases rapidly before, and falls of slowly after."
    )
    private double configIdealDistance;

    @ConfigDoubleList(def = { 1.6, 2.1, 2.7 }, min = 0.0, desc = "Grappling strength for each enchantment level.")
    private List<Double> configStrength;

    public GrapplingHook(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("H", "L", "B")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                .setIngredient('L', Material.LEAD)
                .setIngredient('H', Material.TRIPWIRE_HOOK)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        );
    }

    private double getStrength(int level) {
        if (level > 0 && level <= configStrength.size()) {
            return configStrength.get(level - 1);
        }
        return configStrength.get(0);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerFishEvent(final PlayerFishEvent event) {
        // Get enchantment level
        final var player = event.getPlayer();
        var item = player.getEquipment().getItemInMainHand();
        var level = item.getEnchantmentLevel(this.bukkit());
        if (level == 0) {
            item = player.getEquipment().getItemInOffHand();
            level = item.getEnchantmentLevel(this.bukkit());
            if (level == 0) {
                return;
            }
        }

        switch (event.getState()) {
            case IN_GROUND:
                break;
            case REEL_IN:
                // Check if the hook is colliding with blocks, worldborder, or entities
                // We won't activate the grappling hook if it collides with an entity because the event
                // would instead be in the CAUGHT_ENTITY state
                double hookX = event.getHook().getLocation().getX();
                double hookY = event.getHook().getLocation().getY();
                double hookZ = event.getHook().getLocation().getZ();
                if (!event.getHook().wouldCollideUsing(new BoundingBox(
                        hookX - BOUNDING_BOX_RADIUS, hookY - BOUNDING_BOX_RADIUS, hookZ - BOUNDING_BOX_RADIUS,
                        hookX + BOUNDING_BOX_RADIUS, hookY + BOUNDING_BOX_RADIUS, hookZ + BOUNDING_BOX_RADIUS))
                ) { return; }
                break;
            default:
                return;
        }

        var direction = event.getHook().getLocation().subtract(player.getLocation()).toVector();
        var distance = direction.length();
        var attenuation = distance / configIdealDistance;

        // Reset fall distance
        player.setFallDistance(0.0f);

        var vectorMultiplier = getStrength(level) * Math.exp(1.0 - attenuation) * attenuation;
        var adjustedVector = direction.normalize().multiply(vectorMultiplier).add(CONSTANT_OFFSET);

        // If the hook is below the player, set the Y component to 0.0 and only add the constant offset.
        // This prevents the player from just sliding against the ground when the hook is below them.
        if (player.getY() - event.getHook().getY() > 0) {
            adjustedVector.setY(0.0).add(CONSTANT_OFFSET);
        }

        // Set player velocity
        player.setVelocity(player.getVelocity().add(adjustedVector));
    }
}
