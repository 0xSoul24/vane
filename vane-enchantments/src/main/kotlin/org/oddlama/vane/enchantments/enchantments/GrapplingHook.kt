package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.config.ConfigDoubleList
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments
import kotlin.math.exp

@VaneEnchantment(name = "grappling_hook", maxLevel = 3, rarity = Rarity.UNCOMMON, treasure = true)
class GrapplingHook(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    @ConfigDouble(
        def = 16.0,
        min = 2.0,
        max = 50.0,
        desc = "Ideal grappling distance for maximum grapple strength. Strength increases rapidly before, and falls of slowly after."
    )
    private val configIdealDistance = 0.0

    @ConfigDoubleList(def = [1.6, 2.1, 2.7], min = 0.0, desc = "Grappling strength for each enchantment level.")
    private val configStrength: MutableList<Double?>? = null

    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("H", "L", "B")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                .setIngredient('L', Material.LEAD)
                .setIngredient('H', Material.TRIPWIRE_HOOK)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        )
    }

    private fun getStrength(level: Int): Double {
        if (level > 0 && level <= configStrength!!.size) {
            return configStrength[level - 1]!!
        }
        return configStrength!![0]!!
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerFishEvent(event: PlayerFishEvent) {
        // Get enchantment level
        val player = event.getPlayer()
        var item = player.equipment.itemInMainHand
        var level = item.getEnchantmentLevel(this.bukkit()!!)
        if (level == 0) {
            item = player.equipment.itemInOffHand
            level = item.getEnchantmentLevel(this.bukkit()!!)
            if (level == 0) {
                return
            }
        }

        when (event.state) {
            PlayerFishEvent.State.IN_GROUND -> {}
            PlayerFishEvent.State.REEL_IN -> {
                // Check if the hook is colliding with blocks, worldborder, or entities
                // We won't activate the grappling hook if it collides with an entity because the event
                // would instead be in the CAUGHT_ENTITY state
                val hookX = event.hook.location.x
                val hookY = event.hook.location.y
                val hookZ = event.hook.location.z
                if (!event.hook.wouldCollideUsing(
                        BoundingBox(
                            hookX - BOUNDING_BOX_RADIUS, hookY - BOUNDING_BOX_RADIUS, hookZ - BOUNDING_BOX_RADIUS,
                            hookX + BOUNDING_BOX_RADIUS, hookY + BOUNDING_BOX_RADIUS, hookZ + BOUNDING_BOX_RADIUS
                        )
                    )
                ) {
                    return
                }
            }

            else -> return
        }

        val direction = event.hook.location.subtract(player.location).toVector()
        val distance = direction.length()
        val attenuation = distance / configIdealDistance

        // Reset fall distance
        player.fallDistance = 0.0f

        val vectorMultiplier = getStrength(level) * exp(1.0 - attenuation) * attenuation
        val adjustedVector = direction.normalize().multiply(vectorMultiplier).add(CONSTANT_OFFSET)

        // If the hook is below the player, set the Y component to 0.0 and only add the constant offset.
        // This prevents the player from just sliding against the ground when the hook is below them.
        if (player.y - event.hook.y > 0) {
            adjustedVector.setY(0.0).add(CONSTANT_OFFSET)
        }

        // Set player velocity
        player.velocity = player.velocity.add(adjustedVector)
    }

    companion object {
        // Constant offset to the added velocity, so the player will always move up a little.
        private val CONSTANT_OFFSET = Vector(0.0, 0.2, 0.0)
        private const val BOUNDING_BOX_RADIUS = 0.2
    }
}
