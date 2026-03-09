package org.oddlama.vane.trifles.items

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.Tag
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.config.recipes.SmithingRecipeDefinition
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.BlockUtil
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.MaterialUtil
import org.oddlama.vane.util.PlayerUtil

class Sickles(context: Context<Trifles?>) :
    Listener<Trifles?>(context.group("Sickles", "Several sickles that allow players to harvest crops in a radius.")) {
    @VaneItem(name = "wooden_sickle", base = Material.WOODEN_HOE, modelData = 0x760004, version = 1)
    class WoodenSickle(context: Context<Trifles?>) : Sickle(context) {
        fun configAttackDamageDef(): Double {
            return 1.0
        }

        fun configAttackSpeedDef(): Double {
            return 1.0
        }

        fun configHarvestRadiusDef(): Int {
            return 1
        }

        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Tag.PLANKS)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            )
        }
    }

    @VaneItem(name = "stone_sickle", base = Material.STONE_HOE, modelData = 0x760005, version = 1)
    class StoneSickle(context: Context<Trifles?>) : Sickle(context) {
        fun configAttackDamageDef(): Double {
            return 1.5
        }

        fun configAttackSpeedDef(): Double {
            return 1.5
        }

        fun configHarvestRadiusDef(): Int {
            return 1
        }

        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Tag.ITEMS_STONE_TOOL_MATERIALS)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            )
        }
    }

    @VaneItem(name = "iron_sickle", base = Material.IRON_HOE, modelData = 0x760006, version = 1)
    class IronSickle(context: Context<Trifles?>) : Sickle(context) {
        fun configAttackDamageDef(): Double {
            return 2.0
        }

        fun configAttackSpeedDef(): Double {
            return 2.0
        }

        fun configHarvestRadiusDef(): Int {
            return 2
        }

        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Material.IRON_INGOT)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            )
        }
    }

    @VaneItem(name = "golden_sickle", base = Material.GOLDEN_HOE, modelData = 0x760007, version = 1)
    class GoldenSickle(context: Context<Trifles?>) : Sickle(context) {
        fun configAttackDamageDef(): Double {
            return 1.5
        }

        fun configAttackSpeedDef(): Double {
            return 3.5
        }

        fun configHarvestRadiusDef(): Int {
            return 3
        }

        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Material.GOLD_INGOT)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            )
        }
    }

    @VaneItem(name = "diamond_sickle", base = Material.DIAMOND_HOE, modelData = 0x760008, version = 1)
    class DiamondSickle(context: Context<Trifles?>) : Sickle(context) {
        fun configAttackDamageDef(): Double {
            return 2.5
        }

        fun configAttackSpeedDef(): Double {
            return 2.5
        }

        fun configHarvestRadiusDef(): Int {
            return 2
        }

        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Material.DIAMOND)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            )
        }
    }

    @VaneItem(name = "netherite_sickle", base = Material.NETHERITE_HOE, modelData = 0x760009, version = 1)
    class NetheriteSickle(context: Context<Trifles?>) : Sickle(context) {
        fun configAttackDamageDef(): Double {
            return 3.0
        }

        fun configAttackSpeedDef(): Double {
            return 3.0
        }

        fun configHarvestRadiusDef(): Int {
            return 2
        }

        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                SmithingRecipeDefinition("generic")
                    .base("vane_trifles:diamond_sickle")
                    .addition(Material.NETHERITE_INGOT)
                    .copyNbt(true)
                    .result(key().toString())
            )
        }
    }

    init {
        WoodenSickle(getContext()!!)
        StoneSickle(getContext()!!)
        IronSickle(getContext()!!)
        GoldenSickle(getContext()!!)
        DiamondSickle(getContext()!!)
        NetheriteSickle(getContext()!!)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerRightClickPlant(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        if (event.useItemInHand() == Event.Result.DENY) {
            return
        }

        // Only seed when right-clicking a plant
        val rootBlock = event.clickedBlock
        val plantType = rootBlock!!.type
        if (!MaterialUtil.isSeededPlant(plantType)) {
            return
        }

        // Get item variant
        val player = event.getPlayer()
        val item = player.equipment.getItem(event.hand!!)
        val customItem: CustomItem? = module!!.core?.itemRegistry()?.get(item)
        if (customItem !is Sickle || !customItem.enabled()) {
            return
        }

        var totalHarvested = 0
        // Harvest surroundings
        for (relativePos in BlockUtil.NEAREST_RELATIVE_BLOCKS_FOR_RADIUS[customItem.configHarvestRadius]) {
            val block = BlockUtil.relative(rootBlock, relativePos)
            if (PlayerUtil.harvestPlant(player, block)) {
                ++totalHarvested
            }
        }

        // Damage item if we harvested at least one plant
        if (totalHarvested > 0) {
            ItemUtil.damageItem(player, item, 1 + (0.25 * totalHarvested).toInt())
            PlayerUtil.swingArm(player, event.hand!!)
            rootBlock
                .world
                .playSound(rootBlock.location, Sound.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 2.0f)
        }

        // Prevent offhand from triggering (e.g., placing torches)
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)
    }
}
