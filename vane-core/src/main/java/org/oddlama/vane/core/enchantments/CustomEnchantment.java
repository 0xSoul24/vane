package org.oddlama.vane.core.enchantments;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.config.loot.LootTableList;
import org.oddlama.vane.core.config.loot.LootTables;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.Recipes;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.StorageUtil;

public class CustomEnchantment<T extends Module<T>> extends Listener<T> {

    /**
     * Convert a snake_case string to PascalCase.
     * For example: "life_mending" becomes "LifeMending"
     */
    private static String snakeCaseToPascalCase(String snakeCase) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    // Track instances
    private static final Map<Class<?>, CustomEnchantment<?>> instances = new HashMap<>();

    private VaneEnchantment annotation = getClass().getAnnotation(VaneEnchantment.class);
    private String name;
    private NamespacedKey key;

    public Recipes<T> recipes;
    public LootTables<T> lootTables;

    // Language
    @LangMessage
    public TranslatedMessage langName;

    public CustomEnchantment(Context<T> context) {
        this(context, true);
    }

    public CustomEnchantment(Context<T> context, boolean defaultEnabled) {
        super(null);
        // Make namespace
        name = annotation.name();
        context = context.group("Enchantment" + snakeCaseToPascalCase(name), "Enable enchantment " + name, defaultEnabled);
        setContext(context);

        // Create a namespaced key
        key = StorageUtil.namespacedKey(getModule().namespace(), name);

        // Check if instance already exists
        if (instances.get(getClass()) != null) {
            throw new RuntimeException("Cannot create two instances of a custom enchantment!");
        }
        instances.put(getClass(), this);

        // Automatic recipes and loot table config and registration
        recipes = new Recipes<T>(getContext(), this.key, this::defaultRecipes);
        lootTables = new LootTables<T>(getContext(), this.key, this::defaultLootTables);
    }

    /** Returns the bukkit wrapper for this enchantment. */
    public final Enchantment bukkit() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);
    }

    /** Returns the namespaced key for this enchantment. */
    public final NamespacedKey key() {
        return key;
    }

    /** Only for internal use. */
    final String getName() {
        return name;
    }

    /**
     * Returns the display format for the display name. By default, the color is dependent on the
     * rarity. COMMON: gray UNCOMMON: dark blue RARE: gold VERY_RARE: bold dark purple
     */
    public Component applyDisplayFormat(Component component) {
        switch (annotation.rarity()) {
            default:
            case COMMON:
            case UNCOMMON:
                return component.color(NamedTextColor.DARK_AQUA);
            case RARE:
                return component.color(NamedTextColor.GOLD);
            case VERY_RARE:
                return component.color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD);
        }
    }

    /**
     * Determines the display name of the enchantment. Usually you don't need to override this
     * method, as it already uses clientside translation keys and supports chat formatting.
     */
    public Component displayName(int level) {
        var displayName = applyDisplayFormat(langName.format().decoration(TextDecoration.ITALIC, false));

        if (level != 1 || maxLevel() != 1) {
            final var chatLevel = applyDisplayFormat(
                Component.translatable("enchantment.level." + level).decoration(TextDecoration.ITALIC, false)
            );
            displayName = displayName.append(Component.text(" ")).append(chatLevel);
        }

        return displayName;
    }

    /** The minimum level this enchantment can have. Always fixed to 1. */
    public final int minLevel() {
        return 1;
    }

    /**
     * The maximum level this enchantment can have. Always reflects the annotation value {@link
     * VaneEnchantment#maxLevel()}.
     */
    public final int maxLevel() {
        return annotation.maxLevel();
    }

    /**
     * Determines the minimum enchanting table level at which this enchantment can occur at the
     * given level.
     */
    public int minCost(int level) {
        return 1 + level * 10;
    }

    /**
     * Determines the maximum enchanting table level at which this enchantment can occur at the
     * given level.
     */
    public int maxCost(int level) {
        return minCost(level) + 5;
    }

    /**
     * Determines if this enchantment can be obtained with the enchanting table. Always reflects the
     * annotation value {@link VaneEnchantment#treasure()}.
     */
    public final boolean isTreasure() {
        return annotation.treasure();
    }

    /**
     * Determines if this enchantment is tradeable with villagers. Always reflects the annotation
     * value {@link VaneEnchantment#tradeable()}.
     */
    public final boolean isTradeable() {
        return annotation.tradeable();
    }

    /**
     * Determines if this enchantment is a curse. Always reflects the annotation value {@link
     * VaneEnchantment#curse()}.
     */
    public final boolean isCurse() {
        return annotation.curse();
    }

    /**
     * Determines if this enchantment generates on treasure items. Always reflects the annotation
     * value {@link VaneEnchantment#generateInTreasure()}.
     */
    public final boolean generateInTreasure() {
        return annotation.generateInTreasure();
    }

    /**
     * Determines the enchantment rarity. Always reflects the annotation value {@link
     * VaneEnchantment#rarity()}.
     */
    public final Rarity rarity() {
        return annotation.rarity();
    }

    /** Weather custom items are allowed to be enchanted with this enchantment. */
    public final boolean allowCustom() {
        return annotation.allowCustom();
    }

    /**
     * Determines if this enchantment is compatible with the given enchantment. By default, all
     * enchantments are compatible. Override this if you want to express conflicting enchantments.
     */
    public boolean isCompatible(@NotNull Enchantment other) {
        return true;
    }

    /**
     * Determines if this enchantment can be applied to the given item. By default, this returns
     * true for all items. Item compatibility is now primarily managed by tags in the registry system.
     * This method can still be used for additional custom validation if needed.
     */
    public boolean canEnchant(@NotNull ItemStack itemStack) {
        return true;
    }

    public RecipeList defaultRecipes() {
        return RecipeList.of();
    }

    public LootTableList defaultLootTables() {
        return LootTableList.of();
    }

    /** Applies this enchantment to the given string item definition. */
    protected String on(String itemDefinition) {
        return on(itemDefinition, 1);
    }

    protected String on(String itemDefinition, int level) {
        return itemDefinition + "#enchants{" + key + "*" + level + "}";
    }
}
