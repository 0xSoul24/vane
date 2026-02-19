package org.oddlama.vane.trifles.items;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.trifles.event.PlayerTeleportScrollEvent;
import org.oddlama.vane.util.StorageUtil;

@VaneItem(
    name = "unstable_scroll",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 25,
    modelData = 0x760001,
    version = 1
)
public class UnstableScroll extends Scroll {

    public static final NamespacedKey LAST_SCROLL_TELEPORT_LOCATION = StorageUtil.namespacedKey(
        "vane",
        "last_scroll_teleport_location"
    );

    @LangMessage
    public TranslatedMessage langTeleportNoPreviousTeleport;

    public UnstableScroll(Context<Trifles> context) {
        super(context, 6000);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("ABA", "EPE")
                .setIngredient('P', "vane_trifles:papyrus_scroll")
                .setIngredient('E', Material.CHORUS_FRUIT)
                .setIngredient('A', Material.AMETHYST_SHARD)
                .setIngredient('B', Material.COMPASS)
                .result(key().toString())
        );
    }

    @Override
    public Location teleportLocation(final ItemStack scroll, Player player, boolean imminentTeleport) {
        final var loc = StorageUtil.storageGetLocation(
            player.getPersistentDataContainer(),
            LAST_SCROLL_TELEPORT_LOCATION,
            null
        );
        if (imminentTeleport && loc == null) {
            langTeleportNoPreviousTeleport.sendActionBar(player);
        }
        return loc;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleportScroll(final PlayerTeleportScrollEvent event) {
        StorageUtil.storageSetLocation(
            event.getPlayer().getPersistentDataContainer(),
            LAST_SCROLL_TELEPORT_LOCATION,
            event.getFrom()
        );
    }
}
