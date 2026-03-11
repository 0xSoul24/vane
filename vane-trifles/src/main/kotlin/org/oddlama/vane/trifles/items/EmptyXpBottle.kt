package org.oddlama.vane.trifles.items

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapelessRecipeDefinition
import org.oddlama.vane.core.item.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.trifles.items.XpBottles.XpBottle
import org.oddlama.vane.util.expForLevel
import org.oddlama.vane.util.PlayerUtil
import java.util.*
import kotlin.math.roundToInt

@VaneItem(name = "empty_xp_bottle", base = Material.GLASS_BOTTLE, modelData = 0x76000a, version = 1)
class EmptyXpBottle(context: Context<Trifles?>) : CustomItem<Trifles?>(context) {
    @ConfigDouble(
        def = 0.3,
        min = 0.0,
        max = 0.999,
        desc = "Percentage of experience lost when bottling. For 10% loss, bottling 30 levels will require 30 * (1 / (1 - 0.1)) = 33.33 levels"
    )
    var configLossPercentage: Double = 0.0

    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapelessRecipeDefinition("generic")
                .addIngredient(Material.EXPERIENCE_BOTTLE)
                .addIngredient(Material.GLASS_BOTTLE)
                .addIngredient(Material.GOLD_NUGGET)
                .result(key().toString())
        )
    }

    // Changed return type to non-nullable EnumSet<InhibitBehavior> to match the overridden member
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        return EnumSet.of(
            InhibitBehavior.USE_IN_VANILLA_RECIPE,
            InhibitBehavior.TEMPT,
            InhibitBehavior.USE_OFFHAND
        )
    }

    @EventHandler(
        priority = EventPriority.NORMAL,
        ignoreCancelled = false
    ) // ignoreCancelled = false to catch right-click-air events
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) {
            return
        }

        // Get item variant
        val player = event.getPlayer()
        val item = player.equipment.getItem(event.hand!!)
        if (!isInstance(item)) {
            return
        }

        // Never actually use the base item if it's custom!
        event.setUseItemInHand(Event.Result.DENY)

        when (event.action) {
            Action.RIGHT_CLICK_AIR -> {}
            Action.RIGHT_CLICK_BLOCK ->                 // Require non-canceled state (so it won't trigger for block-actions like chests)
                if (event.useInteractedBlock() != Event.Result.DENY) {
                    return
                }

            else -> return
        }

        // Check if last consume time is too recent, to prevent accidental re-filling
        val now = System.currentTimeMillis()
        val lastConsume = module!!.lastXpBottleConsumeTime.getOrDefault(player.uniqueId, 0L)
        if (now - lastConsume < 1000) {
            return
        }

        // Find maximum fitting capacity
        var xpBottle: XpBottle? = null
        var exp = 0
        for (bottle in module!!.xpBottles!!.bottles) {
            val curExp = ((1.0 / (1.0 - configLossPercentage)) * expForLevel(bottle.configCapacity)).toInt()

            // Check if player has enough xp and this variant has more than the last
            if (getTotalExp(player) >= curExp && curExp > exp) {
                exp = curExp
                xpBottle = bottle
            }
        }

        // Check if there was a fitting bottle
        if (xpBottle == null) {
            return
        }

        // Take xp, take item, play sound, give item.
        player.giveExp(-exp, false)
        PlayerUtil.removeOneItemFromHand(player, event.hand!!)
        PlayerUtil.giveItem(player, xpBottle.newStack())
        player
            .world
            .playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 4.0f)
        PlayerUtil.swingArm(player, event.hand!!)
    }

    companion object {
        fun getTotalExp(player: Player): Int {
            // Use Kotlin's Float.roundToInt() to convert fractional XP to an Int
            return levelToExp(player.level) + (player.expToLevel * player.exp).roundToInt()
        }

        fun levelToExp(level: Int): Int {
            // Formulas taken from: https://minecraft.fandom.com/wiki/Experience#Leveling_up
            if (level > 30) {
                return (4.5 * level * level - 162.5 * level + 2220).toInt()
            }
            if (level > 15) {
                return (2.5 * level * level - 40.5 * level + 360).toInt()
            }
            return level * level + 6 * level
        }
    }
}
