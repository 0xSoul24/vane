package org.oddlama.vane.trifles.items;

import java.util.EnumSet;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.SmithingRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "reinforced_elytra", base = Material.ELYTRA, durability = 864, modelData = 0x760002, version = 1)
public class ReinforcedElytra extends CustomItem<Trifles> {

    @ConfigDouble(def = 6.0, min = 0, desc = "Amount of defense points.")
    private double configDefensePoints;

    public ReinforcedElytra(Context<Trifles> context) {
        super(context);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new SmithingRecipeDefinition("generic")
                .base(Material.ELYTRA)
                .addition(Material.NETHERITE_INGOT)
                .copyNbt(true)
                .result(key().toString())
        );
    }

    @Override
    public ItemStack updateItemStack(ItemStack itemStack) {
        itemStack.editMeta(meta -> {
            final var modifierDefense = new AttributeModifier(
                namespacedKey("armor"),
                    configDefensePoints,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.CHEST
            );
            meta.removeAttributeModifier(Attribute.ARMOR, modifierDefense);
            meta.addAttributeModifier(Attribute.ARMOR, modifierDefense);
        });
        return itemStack;
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.ITEM_BURN);
    }
}
