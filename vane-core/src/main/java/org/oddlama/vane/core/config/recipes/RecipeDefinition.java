package org.oddlama.vane.core.config.recipes;

import static org.oddlama.vane.util.MaterialUtil.materialFrom;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.StorageUtil;

public abstract class RecipeDefinition {

    public String name;

    public RecipeDefinition(final String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public NamespacedKey key(final NamespacedKey baseKey) {
        return StorageUtil.namespacedKey(baseKey.namespace(), baseKey.value() + "." + name);
    }

    public abstract Recipe toRecipe(final NamespacedKey baseKey);

    public abstract Object toDict();

    public abstract RecipeDefinition fromDict(Object dict);

    public static RecipeDefinition fromDict(final String name, final Object dict) {
        if (!(dict instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                "Invalid recipe dictionary: Argument must be a Map<String, Object>, but is " + dict.getClass() + "!"
            );
        }
        final var typeObj = ((Map<?, ?>) dict).containsKey("Type") ? ((Map<?, ?>) dict).get("Type") : ((Map<?, ?>) dict).get("type");
        if (!(typeObj instanceof String)) {
            throw new IllegalArgumentException("Invalid recipe dictionary: recipe type must exist and be a string!");
        }

        final var strType = (String) typeObj;
        switch (strType) {
            case "shaped":
                return new ShapedRecipeDefinition(name).fromDict(dict);
            case "shapeless":
                return new ShapelessRecipeDefinition(name).fromDict(dict);
            case "blasting": // fallthrough
            case "furnace": // fallthrough
            case "campfire": // fallthrough
            case "smoking":
                return new CookingRecipeDefinition(name, strType).fromDict(dict);
            case "smithing":
                return new SmithingRecipeDefinition(name).fromDict(dict);
            case "stonecutting":
                return new StonecuttingRecipeDefinition(name).fromDict(dict);
            default:
                break;
        }

        throw new IllegalArgumentException("Unknown recipe type '" + strType + "'");
    }

    @SuppressWarnings("unchecked")
    public static @NotNull RecipeChoice recipeChoice(String definition) {
        definition = definition.strip();

        // Try a material #tag
        if (definition.startsWith("#")) {
            for (final var f : Tag.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == Tag.class) {
                    try {
                        final var tag = (Tag<?>) f.get(null);
                        if (tag == null) {
                            // System.out.println("warning: " + f + " has no associated key! It
                            // therefore cannot be used in custom recipes.");
                            continue;
                        }
                        if (tag.key().toString().equals(definition.substring(1))) {
                            return new RecipeChoice.MaterialChoice((Tag<Material>) tag);
                        }
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new IllegalArgumentException("Invalid material tag: " + definition);
                    }
                }
            }
            throw new IllegalArgumentException("Unknown material tag: " + definition);
        }

        // Tuple of materials
        if (definition.startsWith("(") && definition.endsWith(")")) {
            final var parts = Arrays.stream(definition.substring(1, definition.length() - 1).split(","))
                .map(key -> {
                    final var mat = materialFrom(NamespacedKey.fromString(key.strip()));
                    if (mat == null) {
                        throw new IllegalArgumentException(
                            "Unknown material (only normal materials are allowed in tags): " + key
                        );
                    }
                    return mat;
                })
                .collect(Collectors.toList());
            return new RecipeChoice.MaterialChoice(parts);
        }

        // Check if the amount is included
        final var mult = definition.indexOf('*');
        int amount = 1;
        if (mult != -1) {
            final var amountStr = definition.substring(0, mult).strip();
            try {
                amount = Integer.parseInt(amountStr);
                if (amount <= 0) {
                    amount = 1;
                }

                // Remove amount from definition for parsing
                definition = definition.substring(mult + 1).strip();
            } catch (NumberFormatException e) {}
        }

        // Exact choice of itemstack including NBT
        final var itemStackAndIsSimpleMat = ItemUtil.itemstackFromString(definition);
        final var itemStack = itemStackAndIsSimpleMat.getLeft();
        final var isSimpleMat = itemStackAndIsSimpleMat.getRight();
        if (isSimpleMat && amount == 1) {
            return new RecipeChoice.MaterialChoice(itemStack.getType());
        }

        itemStack.setAmount(amount);
        return new RecipeChoice.ExactChoice(itemStack);
    }
}
