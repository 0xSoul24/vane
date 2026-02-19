package org.oddlama.vane.core.config.recipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.oddlama.vane.util.ItemUtil;

public class ShapedRecipeDefinition extends RecipeDefinition {

    private List<String> shape = new ArrayList<>();
    private Map<String, String> ingredients = new HashMap<>();
    private String result = null;

    public ShapedRecipeDefinition(String name) {
        super(name);
    }

    public ShapedRecipeDefinition shape(String... shape) {
        this.shape = List.of(shape);
        return this;
    }

    public ShapedRecipeDefinition setIngredient(char id, String ingredient) {
        this.ingredients.put("" + id, ingredient);
        return this;
    }

    public ShapedRecipeDefinition setIngredient(char id, final Tag<?> tag) {
        return setIngredient(id, "#" + tag.key());
    }

    public ShapedRecipeDefinition setIngredient(char id, Material material) {
        return setIngredient(id, material.key().toString());
    }

    public ShapedRecipeDefinition result(String result) {
        this.result = result;
        return this;
    }

    @Override
    public Object toDict() {
        final HashMap<String, Object> dict = new HashMap<>();
        dict.put("Type", "shaped");
        dict.put("Shape", this.shape);
        dict.put("Ingredients", this.ingredients);
        dict.put("Result", this.result);
        return dict;
    }

    @Override
    public RecipeDefinition fromDict(Object dict) {
        if (!(dict instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                "Invalid shaped recipe dictionary: Argument must be a Map<String, Object>!"
            );
        }
        final var dictMap = (Map<?, ?>) dict;
        final Object shapeObj = dictMap.containsKey("Shape") ? dictMap.get("Shape") : dictMap.get("shape");
        if (shapeObj instanceof List<?> shape) {
            this.shape = shape.stream().map(row -> (String) row).toList();
            if (this.shape.size() < 1 && this.shape.size() > 3) {
                throw new IllegalArgumentException(
                    "Invalid shaped recipe dictionary: shape must be a list of 1 to 3 strings"
                );
            }
        } else {
            throw new IllegalArgumentException("Invalid shaped recipe dictionary: shape must be a list of strings");
        }

        final Object ingredientsObj = dictMap.containsKey("Ingredients") ? dictMap.get("Ingredients") : dictMap.get("ingredients");
        if (ingredientsObj instanceof Map<?, ?> ingredients) {
            this.ingredients = ingredients
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
        } else {
            throw new IllegalArgumentException(
                "Invalid shaped recipe dictionary: ingredients must be a mapping of string to string"
            );
        }

        final Object resultObj = dictMap.containsKey("Result") ? dictMap.get("Result") : dictMap.get("result");
        if (resultObj instanceof String result) {
            this.result = result;
        } else {
            throw new IllegalArgumentException("Invalid shaped recipe dictionary: result must be a string");
        }

        return this;
    }

    @Override
    public Recipe toRecipe(NamespacedKey baseKey) {
        final var recipe = new ShapedRecipe(key(baseKey), ItemUtil.itemstackFromString(this.result).getLeft());
        recipe.shape(this.shape.toArray(new String[0]));
        this.ingredients.forEach((name, definition) ->
                recipe.setIngredient(name.charAt(0), RecipeDefinition.recipeChoice(definition))
            );
        return recipe;
    }
}
