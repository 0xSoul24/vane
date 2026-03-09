package org.oddlama.vane.trifles.items

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.CompassMeta
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import java.util.*
import java.util.function.Consumer

@VaneItem(name = "north_compass", base = Material.COMPASS, modelData = 0x760013, version = 1)
class NorthCompass(context: Context<Trifles?>) : org.oddlama.vane.core.item.CustomItem<Trifles?>(context) {
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape(" M ", "MRM", " M ")
                .setIngredient('M', Material.COPPER_INGOT)
                .setIngredient('R', Material.REDSTONE)
                .result(key().toString())
        )
    }

    override fun updateItemStack(itemStack: ItemStack): ItemStack {
        // Use Kotlin properties instead of Java getter-style calls
        val worlds: MutableList<World> = module!!.server.worlds
        if (worlds.isNotEmpty()) {
            val world = worlds[0]
            itemStack.editMeta(CompassMeta::class.java, Consumer { meta: CompassMeta? ->
                meta!!.lodestone = Location(world, 0.0, 0.0, -300000000.0)
                meta.isLodestoneTracked = false
            })
        }
        return itemStack
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerClickInventory(event: InventoryClickEvent) {
        val item = event.getCurrentItem()
        if (item == null || item.type != Material.COMPASS) {
            return
        }

        // Access core via the Kotlin property and use null-safe calls
        val customItem: CustomItem? = module!!.core?.itemRegistry()?.get(item)
        if (customItem !is NorthCompass || !customItem.enabled()) {
            return
        }

        // FIXME: not very performant to do this on every click, but
        // there aren't many options if we want to support other plugins creating
        // this item. (e.g. to allow giving it to players in kits, shops, ...)
        item.editMeta(CompassMeta::class.java, Consumer { meta: CompassMeta? ->
            // Only if it isn't already initialized. This allows making different
            // compasses for different worlds. The world in which it is crafted
            // is stored forever.
            if (!meta!!.hasLodestone()) {
                meta.isLodestoneTracked = false
                meta.lodestone = Location(event.whoClicked.world, 0.0, 0.0, -300000000.0)
            }
        })
    }

    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.USE_OFFHAND)
    }
}
