package org.oddlama.vane.trifles.items;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "north_compass", base = Material.COMPASS, modelData = 0x760013, version = 1)
public class NorthCompass extends CustomItem<Trifles> {

    public NorthCompass(final Context<Trifles> context) {
        super(context);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape(" M ", "MRM", " M ")
                .setIngredient('M', Material.COPPER_INGOT)
                .setIngredient('R', Material.REDSTONE)
                .result(key().toString())
        );
    }

    @Override
    public ItemStack updateItemStack(final ItemStack itemStack) {
        final var worlds = getModule().getServer().getWorlds();
        if (worlds.size() > 0) {
            final var world = worlds.get(0);
            if (world != null) {
                itemStack.editMeta(CompassMeta.class, meta -> {
                    meta.setLodestone(new Location(world, 0.0, 0.0, -300000000.0));
                    meta.setLodestoneTracked(false);
                });
            }
        }
        return itemStack;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerClickInventory(final InventoryClickEvent event) {
        final var item = event.getCurrentItem();
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }

        final var customItem = getModule().getCore().itemRegistry().get(item);
        if (!(customItem instanceof NorthCompass northCompass) || !northCompass.enabled()) {
            return;
        }

        // FIXME: not very performant to do this on every click, but
        // there aren't many options if we want to support other plugins creating
        // this item. (e.g. to allow giving it to players in kits, shops, ...)
        item.editMeta(CompassMeta.class, meta -> {
            // Only if it isn't already initialized. This allows making different
            // compasses for different worlds. The world in which it is crafted
            // is stored forever.
            if (!meta.hasLodestone()) {
                meta.setLodestoneTracked(false);
                meta.setLodestone(new Location(event.getWhoClicked().getWorld(), 0.0, 0.0, -300000000.0));
            }
        });
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.USE_OFFHAND);
    }
}
