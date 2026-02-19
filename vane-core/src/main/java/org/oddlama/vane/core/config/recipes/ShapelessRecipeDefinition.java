package org.oddlama.vane.core.config.recipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.oddlama.vane.util.ItemUtil;

public class ShapelessRecipeDefinition extends RecipeDefinition {

    private List<String> ingredients = new ArrayList<>();
    private String result = null;

    public ShapelessRecipeDefinition(String name) {
        super(name);
    }

    public ShapelessRecipeDefinition addIngredient(String ingredient) {
        this.ingredients.add(ingredient);
        return this;
    }

    public ShapelessRecipeDefinition addIngredient(final Tag<?> tag) {
        return addIngredient("#" + tag.key());
    }

    public ShapelessRecipeDefinition addIngredient(Material material) {
        return addIngredient(material.key().toString());
    }

    public ShapelessRecipeDefinition result(String result) {
        this.result = result;
        return this;
    }

    @Override
    public Object toDict() {
        final HashMap<String, Object> dict = new HashMap<>();
        dict.put("Type", "shapeless");
        dict.put("Ingredients", this.ingredients);
        dict.put("Result", this.result);
        return dict;
    }

    @Override
    public RecipeDefinition fromDict(Object dict) {
        if (!(dict instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                "Invalid shapeless recipe dictionary: Argument must be a Map<String, Object>!"
            );
        }
        final var dictMap = (Map<?, ?>) dict;
        final Object ingredientsObj = dictMap.containsKey("Ingredients") ? dictMap.get("Ingredients") : dictMap.get("ingredients");
        if (ingredientsObj instanceof List<?> ingredients) {
            this.ingredients = ingredients.stream().map(i -> (String) i).toList();
        } else {
            throw new IllegalArgumentException(
                "Invalid shapeless recipe dictionary: ingredients must be a list of strings"
            );
        }

        final Object resultObj = dictMap.containsKey("Result") ? dictMap.get("Result") : dictMap.get("result");
        if (resultObj instanceof String result) {
            this.result = result;
        } else {
            throw new IllegalArgumentException("Invalid shapeless recipe dictionary: result must be a string");
        }

        return this;
    }

    @Override
    public Recipe toRecipe(NamespacedKey baseKey) {
        final var recipe = new ShapelessRecipe(key(baseKey), ItemUtil.itemstackFromString(this.result).getLeft());
        this.ingredients.forEach(i -> recipe.addIngredient(RecipeDefinition.recipeChoice(i)));
        return recipe;
    }
}
