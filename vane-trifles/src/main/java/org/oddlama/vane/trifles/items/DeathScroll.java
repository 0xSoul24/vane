package org.oddlama.vane.trifles.items;

import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.StorageUtil;

@VaneItem(
    name = "death_scroll",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 2,
    modelData = 0x760012,
    version = 1
)
public class DeathScroll extends Scroll {

    public static final NamespacedKey RECENT_DEATH_LOCATION = StorageUtil.namespacedKey(
        "vane",
        "recent_death_location"
    );
    public static final NamespacedKey RECENT_DEATH_TIME = StorageUtil.namespacedKey("vane", "recent_death_time");

    @LangMessage
    public TranslatedMessage langTeleportNoRecentDeath;

    public DeathScroll(Context<Trifles> context) {
        super(context, 6000);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("ABA", "EPE")
                .setIngredient('P', "vane_trifles:papyrus_scroll")
                .setIngredient('E', Material.ENDER_PEARL)
                .setIngredient('A', Material.BONE)
                .setIngredient('B', Material.RECOVERY_COMPASS)
                .result(key().toString())
        );
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        final var set = super.inhibitedBehaviors();
        // Fuck no, this will not be made unbreakable.
        set.add(InhibitBehavior.NEW_ENCHANTS);
        return set;
    }

    @Override
    public Location teleportLocation(final ItemStack scroll, Player player, boolean imminentTeleport) {
        final var pdc = player.getPersistentDataContainer();
        final var time = pdc.getOrDefault(RECENT_DEATH_TIME, PersistentDataType.LONG, 0l);
        var loc = StorageUtil.storageGetLocation(player.getPersistentDataContainer(), RECENT_DEATH_LOCATION, null);

        // Only recent deaths up to 20 minutes ago
        if (System.currentTimeMillis() - time > 20 * 60 * 1000l) {
            loc = null;
        }

        if (imminentTeleport) {
            if (loc == null) {
                langTeleportNoRecentDeath.sendActionBar(player);
            } else {
                // Only once
                pdc.remove(RECENT_DEATH_TIME);
                StorageUtil.storageRemoveLocation(pdc, RECENT_DEATH_LOCATION);
            }
        }

        return loc;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final var pdc = event.getPlayer().getPersistentDataContainer();
        StorageUtil.storageSetLocation(pdc, RECENT_DEATH_LOCATION, event.getPlayer().getLocation());
        pdc.set(RECENT_DEATH_TIME, PersistentDataType.LONG, System.currentTimeMillis());
        event.getPlayer().setCooldown(this.baseMaterial(), 0);
    }
}
