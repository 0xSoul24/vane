package org.oddlama.vane.trifles.items;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(
    name = "home_scroll",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 25,
    modelData = 0x760000,
    version = 1
)
public class HomeScroll extends Scroll {

    public HomeScroll(Context<Trifles> context) {
        super(context, 10000);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("ABC", "EPE")
                .setIngredient('P', "vane_trifles:papyrus_scroll")
                .setIngredient('E', Material.ENDER_PEARL)
                .setIngredient('A', Material.CAMPFIRE)
                .setIngredient('B', Material.GOAT_HORN)
                .setIngredient('C', Tag.BEDS)
                .result(key().toString())
        );
    }

    // @Override
    // public LootTableList defaultLootTables() {
    //	// TODO spawn scroll with 1 usage! possible with nbt nice.
    // }

    @Override
    public Location teleportLocation(final ItemStack scroll, Player player, boolean imminentTeleport) {
        final var toLocation = player.getRespawnLocation();
        if (imminentTeleport && toLocation == null) {
            // replaced deprecated call with getRespawnLocation()
            final var toPotentialLocation = player.getRespawnLocation();
            if (toPotentialLocation != null) {
                // "You have no home bed or charge respawn anchor, or it was obstructed"
                // The most cursed sentence in minecraft.
                player.sendActionBar(Component.translatable("block.minecraft.spawn.not_valid"));
            } else {
                // "Sleep in a bed to change your respawn point."
                player.sendActionBar(Component.translatable("advancements.adventure.sleep_in_bed.description"));
            }
        }
        return toLocation;
    }
}
