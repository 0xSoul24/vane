package org.oddlama.vane.enchantments.items;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.config.loot.LootDefinition;
import org.oddlama.vane.core.config.loot.LootTableList;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.config.recipes.ShapelessRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemHelper;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleGroup;
import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.util.StorageUtil;

public class Tomes extends ModuleGroup<Enchantments> {

    public Tomes(Context<Enchantments> context) {
        super(
            context,
            "Tomes",
            "These tomes are needed to craft custom enchantments. If you disable them here, you will need to adjust the recipes for the enchantments accordingly."
        );
        new GrindstoneListener(getContext());
        new AncientTome(getContext());
        new EnchantedAncientTome(getContext());
        new AncientTomeOfKnowledge(getContext());
        new EnchantedAncientTomeOfKnowledge(getContext());
        new AncientTomeOfTheGods(getContext());
        new EnchantedAncientTomeOfTheGods(getContext());
    }

    @VaneItem(name = "ancient_tome", base = Material.BOOK, modelData = 0x770000, version = 1)
    public static class AncientTome extends CustomItem<Enchantments> {

        public AncientTome(Context<Enchantments> context) {
            super(context);
        }

        @Override
        public LootTableList defaultLootTables() {
            return LootTableList.of(
                new LootDefinition("generic")
                    .in(LootTables.ABANDONED_MINESHAFT)
                    .in(LootTables.BASTION_BRIDGE)
                    .in(LootTables.BASTION_HOGLIN_STABLE)
                    .in(LootTables.BASTION_OTHER)
                    .in(LootTables.BASTION_TREASURE)
                    .in(LootTables.BURIED_TREASURE)
                    .in(LootTables.DESERT_PYRAMID)
                    .in(LootTables.END_CITY_TREASURE)
                    .in(LootTables.FISHING_TREASURE)
                    .in(LootTables.IGLOO_CHEST)
                    .in(LootTables.JUNGLE_TEMPLE)
                    .in(LootTables.NETHER_BRIDGE)
                    .in(LootTables.PILLAGER_OUTPOST)
                    .in(LootTables.RUINED_PORTAL)
                    .in(LootTables.SHIPWRECK_TREASURE)
                    .in(LootTables.STRONGHOLD_LIBRARY)
                    .in(LootTables.UNDERWATER_RUIN_BIG)
                    .in(LootTables.UNDERWATER_RUIN_SMALL)
                    .in(LootTables.VILLAGE_TEMPLE)
                    .in(LootTables.WOODLAND_MANSION)
                    .add(1.0 / 5, 0, 2, key().toString()),
                new LootDefinition("ancientcity").in(LootTables.ANCIENT_CITY).add(1.0 / 20, 0, 2, key().toString()),
                new LootDefinition("terralith_generic")
                    // terralith low
                    .in(StorageUtil.namespacedKey("terralith", "spire/common"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/generic_low"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/generic_low"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/smith/novice"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/smith/novice"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/tavern_downstairs"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/tavern_upstairs"))
                    // terralith normal
                    .in(StorageUtil.namespacedKey("terralith", "ruin/glacial/main_cs"))
                    .in(StorageUtil.namespacedKey("terralith", "spire/treasure"))
                    .in(StorageUtil.namespacedKey("terralith", "underground/chest"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/archer"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/attic"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/butcher"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/cartographer"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/generic"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/library"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/mason"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/smith"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/treasure"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/archer"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/attic"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/butcher"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/cartographer"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/fisherman"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/food"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/generic"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/library"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/mason"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/smith"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/treasure"))
                    .in(StorageUtil.namespacedKey("terralith", "village/treasure/diamond"))
                    .in(StorageUtil.namespacedKey("terralith", "village/treasure/emerald"))
                    .in(StorageUtil.namespacedKey("terralith", "village/treasure/golem"))
                    .add(1.0 / 5, 0, 2, key().toString()),
                new LootDefinition("terralith_rare")
                    // terralith rare
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/smith/expert"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/smith/expert"))
                    .in(StorageUtil.namespacedKey("terralith", "spire/rare"))
                    .add(1.0 / 20, 0, 2, key().toString())
            );
        }
    }

    @VaneItem(name = "enchanted_ancient_tome", base = Material.ENCHANTED_BOOK, modelData = 0x770001, version = 1)
    public static class EnchantedAncientTome extends CustomItem<Enchantments> {

        public EnchantedAncientTome(Context<Enchantments> context) {
            super(context);
        }
    }

    @VaneItem(name = "ancient_tome_of_knowledge", base = Material.BOOK, modelData = 0x770002, version = 1)
    public static class AncientTomeOfKnowledge extends CustomItem<Enchantments> {

        public AncientTomeOfKnowledge(Context<Enchantments> context) {
            super(context);
        }

        @Override
        public RecipeList defaultRecipes() {
            return RecipeList.of(
                new ShapelessRecipeDefinition("generic")
                    .addIngredient("vane_enchantments:ancient_tome")
                    .addIngredient(Material.FEATHER)
                    .addIngredient(Material.BLAZE_ROD)
                    .addIngredient(Material.GHAST_TEAR)
                    .result(key().toString())
            );
        }

        @Override
        public LootTableList defaultLootTables() {
            return LootTableList.of(
                new LootDefinition("generic")
                    .in(LootTables.ABANDONED_MINESHAFT)
                    .in(LootTables.BASTION_TREASURE)
                    .in(LootTables.BURIED_TREASURE)
                    .in(LootTables.DESERT_PYRAMID)
                    .in(LootTables.NETHER_BRIDGE)
                    .in(LootTables.RUINED_PORTAL)
                    .in(LootTables.SHIPWRECK_TREASURE)
                    .in(LootTables.STRONGHOLD_LIBRARY)
                    .in(LootTables.UNDERWATER_RUIN_BIG)
                    .in(LootTables.VILLAGE_TEMPLE)
                    .in(LootTables.WOODLAND_MANSION)
                    .add(1.0 / 40, 1, 1, key().toString()),
                new LootDefinition("ancientcity")
                    .in(LootTables.ANCIENT_CITY)
                    .add(1.0 / 30, 1, 1, key().toString()) // duplicate for more consistent spawn
                    .add(1.0 / 30, 1, 1, key().toString()),
                new LootDefinition("terralith_generic")
                    // terralith normal
                    .in(StorageUtil.namespacedKey("terralith", "ruin/glacial/main_cs"))
                    .in(StorageUtil.namespacedKey("terralith", "spire/treasure"))
                    .in(StorageUtil.namespacedKey("terralith", "underground/chest"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/archer"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/attic"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/butcher"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/cartographer"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/generic"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/library"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/mason"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/smith"))
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/treasure"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/archer"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/attic"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/butcher"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/cartographer"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/fisherman"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/food"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/generic"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/library"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/mason"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/smith"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/treasure"))
                    .in(StorageUtil.namespacedKey("terralith", "village/treasure/diamond"))
                    .in(StorageUtil.namespacedKey("terralith", "village/treasure/emerald"))
                    .in(StorageUtil.namespacedKey("terralith", "village/treasure/golem"))
                    .add(1.0 / 40, 1, 1, key().toString()),
                new LootDefinition("terralith_rare")
                    // terralith rare
                    .in(StorageUtil.namespacedKey("terralith", "village/desert/smith/expert"))
                    .in(StorageUtil.namespacedKey("terralith", "village/fortified/smith/expert"))
                    .in(StorageUtil.namespacedKey("terralith", "spire/rare"))
                    .add(1.0 / 30, 1, 1, key().toString()) // duplicate for more consistent spawn
                    .add(1.0 / 30, 1, 1, key().toString())
            );
        }
    }

    @VaneItem(
        name = "enchanted_ancient_tome_of_knowledge",
        base = Material.ENCHANTED_BOOK,
        modelData = 0x770003,
        version = 1
    )
    public static class EnchantedAncientTomeOfKnowledge extends CustomItem<Enchantments> {

        public EnchantedAncientTomeOfKnowledge(Context<Enchantments> context) {
            super(context);
        }
    }

    @VaneItem(name = "ancient_tome_of_the_gods", base = Material.BOOK, modelData = 0x770004, version = 1)
    public static class AncientTomeOfTheGods extends CustomItem<Enchantments> {

        public AncientTomeOfTheGods(Context<Enchantments> context) {
            super(context);
        }

        @Override
        public RecipeList defaultRecipes() {
            return RecipeList.of(
                new ShapedRecipeDefinition("generic")
                    .shape(" S ", "EBE", " N ")
                    .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                    .setIngredient('E', Material.ENCHANTED_BOOK)
                    .setIngredient('S', Material.NETHER_STAR)
                    .setIngredient('N', Material.NAUTILUS_SHELL)
                    .result(key().toString())
            );
        }

        @Override
        public LootTableList defaultLootTables() {
            return LootTableList.of(
                new LootDefinition("generic")
                    .in(LootTables.BASTION_TREASURE)
                    .in(LootTables.BURIED_TREASURE)
                    .in(LootTables.SHIPWRECK_TREASURE)
                    .in(LootTables.UNDERWATER_RUIN_BIG)
                    .add(1.0 / 200, 1, 1, key().toString()),
                new LootDefinition("ancientcity").in(LootTables.ANCIENT_CITY).add(1.0 / 150, 1, 1, key().toString()),
                new LootDefinition("terralith_generic")
                    // terralith normal
                    .in(StorageUtil.namespacedKey("terralith", "spire/treasure"))
                    .in(StorageUtil.namespacedKey("terralith", "underground/chest"))
                    .add(1.0 / 200, 1, 1, key().toString()),
                new LootDefinition("terralith_rare")
                    // terralith rare
                    .in(StorageUtil.namespacedKey("terralith", "spire/rare"))
                    .add(1.0 / 150, 1, 1, key().toString())
            );
        }
    }

    @VaneItem(
        name = "enchanted_ancient_tome_of_the_gods",
        base = Material.ENCHANTED_BOOK,
        modelData = 0x770005,
        version = 1
    )
    public static class EnchantedAncientTomeOfTheGods extends CustomItem<Enchantments> {

        public EnchantedAncientTomeOfTheGods(Context<Enchantments> context) {
            super(context);
        }
    }

    @VaneItem(
        name = "enchanted_ancient_tome_of_the_gods",
        base = Material.ENCHANTED_BOOK,
        modelData = 0x770005,
        version = 1
    )
    public static class GrindstoneListener extends Listener<Enchantments> {

        public GrindstoneListener(Context<Enchantments> context) {
            super(context);
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
            // Make sure to remove the enchanted variant when disenchanting a tome
            var res = event.getResult();
            if (res == null) {
                return;
            }

            // Only if there are no enchantments on an enchanted variant, we revert to
            // non-enchanted variant
            if (!res.getEnchantments().isEmpty()) {
                return;
            }

            final var customItem = getModule().core.itemRegistry().get(res);
            if (customItem instanceof EnchantedAncientTome) {
                event.setResult(CustomItemHelper.newStack("vane_enchantments:ancient_tome"));
            } else if (customItem instanceof EnchantedAncientTomeOfKnowledge) {
                event.setResult(CustomItemHelper.newStack("vane_enchantments:ancient_tome_of_knowledge"));
            } else if (customItem instanceof EnchantedAncientTomeOfTheGods) {
                event.setResult(CustomItemHelper.newStack("vane_enchantments:ancient_tome_of_the_gods"));
            }
        }
    }
}
