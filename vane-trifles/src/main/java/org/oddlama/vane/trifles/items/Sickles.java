package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.BlockUtil.relative;
import static org.oddlama.vane.util.ItemUtil.damageItem;
import static org.oddlama.vane.util.MaterialUtil.isSeededPlant;
import static org.oddlama.vane.util.PlayerUtil.harvestPlant;
import static org.oddlama.vane.util.PlayerUtil.swingArm;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.config.recipes.SmithingRecipeDefinition;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.BlockUtil;

public class Sickles extends Listener<Trifles> {

    @VaneItem(name = "wooden_sickle", base = Material.WOODEN_HOE, modelData = 0x760004, version = 1)
    public static class WoodenSickle extends Sickle {

        public WoodenSickle(Context<Trifles> context) {
            super(context);
        }

        public double configAttackDamageDef() {
            return 1.0;
        }

        public double configAttackSpeedDef() {
            return 1.0;
        }

        public int configHarvestRadiusDef() {
            return 1;
        }

        @Override
        public RecipeList defaultRecipes() {
            return RecipeList.of(
                new ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Tag.PLANKS)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            );
        }
    }

    @VaneItem(name = "stone_sickle", base = Material.STONE_HOE, modelData = 0x760005, version = 1)
    public static class StoneSickle extends Sickle {

        public StoneSickle(Context<Trifles> context) {
            super(context);
        }

        public double configAttackDamageDef() {
            return 1.5;
        }

        public double configAttackSpeedDef() {
            return 1.5;
        }

        public int configHarvestRadiusDef() {
            return 1;
        }

        @Override
        public RecipeList defaultRecipes() {
            return RecipeList.of(
                new ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Tag.ITEMS_STONE_TOOL_MATERIALS)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            );
        }
    }

    @VaneItem(name = "iron_sickle", base = Material.IRON_HOE, modelData = 0x760006, version = 1)
    public static class IronSickle extends Sickle {

        public IronSickle(Context<Trifles> context) {
            super(context);
        }

        public double configAttackDamageDef() {
            return 2.0;
        }

        public double configAttackSpeedDef() {
            return 2.0;
        }

        public int configHarvestRadiusDef() {
            return 2;
        }

        @Override
        public RecipeList defaultRecipes() {
            return RecipeList.of(
                new ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Material.IRON_INGOT)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            );
        }
    }

    @VaneItem(name = "golden_sickle", base = Material.GOLDEN_HOE, modelData = 0x760007, version = 1)
    public static class GoldenSickle extends Sickle {

        public GoldenSickle(Context<Trifles> context) {
            super(context);
        }

        public double configAttackDamageDef() {
            return 1.5;
        }

        public double configAttackSpeedDef() {
            return 3.5;
        }

        public int configHarvestRadiusDef() {
            return 3;
        }

        @Override
        public RecipeList defaultRecipes() {
            return RecipeList.of(
                new ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Material.GOLD_INGOT)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            );
        }
    }

    @VaneItem(name = "diamond_sickle", base = Material.DIAMOND_HOE, modelData = 0x760008, version = 1)
    public static class DiamondSickle extends Sickle {

        public DiamondSickle(Context<Trifles> context) {
            super(context);
        }

        public double configAttackDamageDef() {
            return 2.5;
        }

        public double configAttackSpeedDef() {
            return 2.5;
        }

        public int configHarvestRadiusDef() {
            return 2;
        }

        @Override
        public RecipeList defaultRecipes() {
            return RecipeList.of(
                new ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Material.DIAMOND)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            );
        }
    }

    @VaneItem(name = "netherite_sickle", base = Material.NETHERITE_HOE, modelData = 0x760009, version = 1)
    public static class NetheriteSickle extends Sickle {

        public NetheriteSickle(Context<Trifles> context) {
            super(context);
        }

        public double configAttackDamageDef() {
            return 3.0;
        }

        public double configAttackSpeedDef() {
            return 3.0;
        }

        public int configHarvestRadiusDef() {
            return 2;
        }

        @Override
        public RecipeList defaultRecipes() {
            return RecipeList.of(
                new SmithingRecipeDefinition("generic")
                    .base("vane_trifles:diamond_sickle")
                    .addition(Material.NETHERITE_INGOT)
                    .copyNbt(true)
                    .result(key().toString())
            );
        }
    }

    public Sickles(Context<Trifles> context) {
        super(context.group("Sickles", "Several sickles that allow players to harvest crops in a radius."));
        new WoodenSickle(getContext());
        new StoneSickle(getContext());
        new IronSickle(getContext());
        new GoldenSickle(getContext());
        new DiamondSickle(getContext());
        new NetheriteSickle(getContext());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerRightClickPlant(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        // Only seed when right-clicking a plant
        final var rootBlock = event.getClickedBlock();
        final var plantType = rootBlock.getType();
        if (!isSeededPlant(plantType)) {
            return;
        }

        // Get item variant
        final var player = event.getPlayer();
        final var item = player.getEquipment().getItem(event.getHand());
        final var customItem = getModule().core.itemRegistry().get(item);
        if (!(customItem instanceof Sickle sickle) || !sickle.enabled()) {
            return;
        }

        var totalHarvested = 0;
        // Harvest surroundings
        for (var relativePos : BlockUtil.NEAREST_RELATIVE_BLOCKS_FOR_RADIUS.get(sickle.configHarvestRadius)) {
            final var block = relative(rootBlock, relativePos);
            if (harvestPlant(player, block)) {
                ++totalHarvested;
            }
        }

        // Damage item if we harvested at least one plant
        if (totalHarvested > 0) {
            damageItem(player, item, 1 + (int) (0.25 * totalHarvested));
            swingArm(player, event.getHand());
            rootBlock
                .getWorld()
                .playSound(rootBlock.getLocation(), Sound.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 2.0f);
        }

        // Prevent offhand from triggering (e.g., placing torches)
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }
}
