package org.oddlama.vane.trifles.items;

import java.util.EnumSet;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.BlockUtil;

public abstract class Sickle extends CustomItem<Trifles> {

    @ConfigDouble(def = Double.NaN, desc = "Attack damage modifier.")
    public double configAttackDamage;

    @ConfigDouble(def = Double.NaN, desc = "Attack speed modifier.")
    public double configAttackSpeed;

    @ConfigInt(def = -1, min = 0, max = BlockUtil.NEAREST_RELATIVE_BLOCKS_FOR_RADIUS_MAX, desc = "Harvest radius.")
    public int configHarvestRadius;

    public Sickle(Context<Trifles> context) {
        super(context);
    }

    @Override
    public ItemStack updateItemStack(ItemStack itemStack) {
        itemStack.editMeta(meta -> {
            final var modifierDamage = new AttributeModifier(
                namespacedKey("attack_damage"),
                    configAttackDamage,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.HAND
            );
            final var modifierSpeed = new AttributeModifier(
                namespacedKey("attack_speed"),
                    configAttackSpeed,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.HAND
            );
            meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE, modifierDamage);
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifierDamage);
            meta.removeAttributeModifier(Attribute.ATTACK_SPEED, modifierSpeed);
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, modifierSpeed);
        });
        return itemStack;
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.HOE_TILL, InhibitBehavior.USE_OFFHAND);
    }
}
