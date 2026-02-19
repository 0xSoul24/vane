package org.oddlama.vane.enchantments;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryComposeEvent;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;

public abstract class CustomEnchantmentRegistry {

    public static final String NAMESPACE = "vane_enchantments";
    Key key;
    Component description;
    int maxLevel;

    TagKey<ItemType> supportedItemTags;
    List<TypedKey<ItemType>> supportedItems = List.of();

    TagKey<Enchantment> exclusiveWithTags;
    List<TypedKey<Enchantment>> exclusiveWith = List.of();

    public CustomEnchantmentRegistry(String name, TagKey<ItemType> supportedItemTags, int maxLevel) {
        this.key = Key.key(NAMESPACE, name);
        final var pascal = snakeCaseToPascalCase(name);
        this.description = Component.translatable(NAMESPACE + ".Enchantment" + pascal + ".Name");
        this.supportedItemTags = supportedItemTags;
        this.maxLevel = maxLevel;
    }

    public CustomEnchantmentRegistry(String name, List<TypedKey<ItemType>> supportedItems, int maxLevel) {
        this.key = Key.key(NAMESPACE, name);
        final var pascal = snakeCaseToPascalCase(name);
        this.description = Component.translatable(NAMESPACE + ".Enchantment" + pascal + ".Name");
        this.supportedItems = supportedItems;
        this.maxLevel = maxLevel;
    }

    /**
     * Add exclusive enchantments to this enchantment: exclusive enchantments can't be on the same
     * tool.
     */
    public CustomEnchantmentRegistry exclusiveWith(List<TypedKey<Enchantment>> enchantments) {
        this.exclusiveWith = enchantments;
        return this;
    }

    /**
     * Add exclusive enchantment <b>tag</b> to this enchantment: exclusive enchantments can't be on
     * the same tool.
     */
    public CustomEnchantmentRegistry exclusiveWith(TagKey<Enchantment> enchantmentTag) {
        this.exclusiveWithTags = enchantmentTag;
        return this;
    }

    /** Get exclusive enchantments */
    public RegistryKeySet<Enchantment> exclusiveWith(
        RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> composeEvent
    ) {
        if (this.exclusiveWithTags != null) {
            return composeEvent.getOrCreateTag(exclusiveWithTags);
        } else {
            return RegistrySet.keySet(RegistryKey.ENCHANTMENT, this.exclusiveWith);
        }
    }

    /**
     * Register the enchantment in the registry
     *
     * @see <a href="https://docs.papermc.io/paper/dev/registries#create-new-entries">Paper Registry Documentation</a>
     */
    public void register(RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> composeEvent) {
        composeEvent
            .registry()
            .register(TypedKey.create(RegistryKey.ENCHANTMENT, key), e ->
                e
                    .description(description)
                    .supportedItems(
                        supportedItems.size() > 0
                            ? RegistrySet.keySet(RegistryKey.ITEM, supportedItems)
                            : composeEvent.getOrCreateTag(supportedItemTags)
                    )
                    .anvilCost(1)
                    .maxLevel(maxLevel)
                    .weight(10)
                    .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 1))
                    .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(3, 1))
                    .activeSlots(EquipmentSlotGroup.ANY)
                    .exclusiveWith(this.exclusiveWith(composeEvent))
            );
    }

    public TypedKey<Enchantment> typedKey(String name) {
        return TypedKey.create(RegistryKey.ENCHANTMENT, Key.key(NAMESPACE, name));
    }

    // Utility: convert snake_case names like "lightning" or "life_mending" to PascalCase "Lightning"/"LifeMending"
    private static String snakeCaseToPascalCase(String snake) {
        final var parts = snake.split("_");
        final var sb = new StringBuilder();
        for (var part : parts) {
            if (part == null || part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
        }
        return sb.toString();
    }
}
