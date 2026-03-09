package org.oddlama.vane.trifles.items

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.trifles.commands.Setspawn

@VaneItem(
    name = "spawn_scroll",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 40,
    modelData = 0x760010,
    version = 1
)
class SpawnScroll(context: Context<Trifles?>) : Scroll(context, 6000) {
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("ABA", "EPE")
                .setIngredient('P', "vane_trifles:papyrus_scroll")
                .setIngredient('E', Material.ENDER_PEARL)
                .setIngredient('A', Material.WHEAT_SEEDS)
                .setIngredient('B', Tag.SAPLINGS)
                .result(key().toString())
        )
    }

    override fun teleportLocation(scroll: ItemStack?, player: Player?, imminentTeleport: Boolean): Location {
        var loc: Location? = null
        for (world in module!!.server.worlds) {
            if (world
                    .persistentDataContainer
                    .getOrDefault(Setspawn.IS_SPAWN_WORLD, PersistentDataType.INTEGER, 0) ==
                1
            ) {
                loc = world.spawnLocation
            }
        }
        // Fallback to spawn location of the first world
        if (loc == null) {
            loc = module!!.server.worlds[0].spawnLocation
        }
        return loc
    }
}
