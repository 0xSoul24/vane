package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.Conversions.expForLevel;
import static org.oddlama.vane.util.PlayerUtil.giveItem;
import static org.oddlama.vane.util.PlayerUtil.removeOneItemFromHand;
import static org.oddlama.vane.util.PlayerUtil.swingArm;

import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapelessRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.trifles.items.XpBottles.XpBottle;

@VaneItem(name = "empty_xp_bottle", base = Material.GLASS_BOTTLE, modelData = 0x76000a, version = 1)
public class EmptyXpBottle extends CustomItem<Trifles> {

    @ConfigDouble(
        def = 0.3,
        min = 0.0,
        max = 0.999,
        desc = "Percentage of experience lost when bottling. For 10% loss, bottling 30 levels will require 30 * (1 / (1 - 0.1)) = 33.33 levels"
    )
    public double configLossPercentage;

    public EmptyXpBottle(Context<Trifles> context) {
        super(context);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapelessRecipeDefinition("generic")
                .addIngredient(Material.EXPERIENCE_BOTTLE)
                .addIngredient(Material.GLASS_BOTTLE)
                .addIngredient(Material.GOLD_NUGGET)
                .result(key().toString())
        );
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.TEMPT, InhibitBehavior.USE_OFFHAND);
    }

    public static int getTotalExp(final Player player) {
        return levelToExp(player.getLevel()) + Math.round(player.getExpToLevel() * player.getExp());
    }

    public static int levelToExp(int level) {
        // Formulas taken from: https://minecraft.fandom.com/wiki/Experience#Leveling_up
        if (level > 30) {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
        if (level > 15) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return level * level + 6 * level;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false) // ignoreCancelled = false to catch right-click-air events
    public void onPlayerRightClick(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        // Get item variant
        final var player = event.getPlayer();
        final var item = player.getEquipment().getItem(event.getHand());
        if (!isInstance(item)) {
            return;
        }

        // Never actually use the base item if it's custom!
        event.setUseItemInHand(Event.Result.DENY);

        switch (event.getAction()) {
            default:
                return;
            case RIGHT_CLICK_AIR:
                break;
            case RIGHT_CLICK_BLOCK:
                // Require non-canceled state (so it won't trigger for block-actions like chests)
                if (event.useInteractedBlock() != Event.Result.DENY) {
                    return;
                }
                break;
        }

        // Check if last consume time is too recent, to prevent accidental re-filling
        final var now = System.currentTimeMillis();
        final var lastConsume = getModule().lastXpBottleConsumeTime.getOrDefault(player.getUniqueId(), 0l);
        if (now - lastConsume < 1000) {
            return;
        }

        // Find maximum fitting capacity
        XpBottle xpBottle = null;
        int exp = 0;
        for (final var bottle : getModule().xpBottles.bottles) {
            var curExp = (int) ((1.0 / (1.0 - configLossPercentage)) * expForLevel(bottle.configCapacity));

            // Check if player has enough xp and this variant has more than the last
            if (getTotalExp(player) >= curExp && curExp > exp) {
                exp = curExp;
                xpBottle = bottle;
            }
        }

        // Check if there was a fitting bottle
        if (xpBottle == null) {
            return;
        }

        // Take xp, take item, play sound, give item.
        player.giveExp(-exp, false);
        removeOneItemFromHand(player, event.getHand());
        giveItem(player, xpBottle.newStack());
        player
            .getWorld()
            .playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 4.0f);
        swingArm(player, event.getHand());
    }
}
