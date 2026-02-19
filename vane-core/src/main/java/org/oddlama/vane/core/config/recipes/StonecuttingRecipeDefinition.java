package org.oddlama.vane.core.config.recipes;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.oddlama.vane.util.ItemUtil;

public class StonecuttingRecipeDefinition extends RecipeDefinition {

    private String input = null;
    private String result = null;

    public StonecuttingRecipeDefinition(String name) {
        super(name);
    }

    public StonecuttingRecipeDefinition input(String input) {
        this.input = input;
        return this;
    }

    public StonecuttingRecipeDefinition input(final Tag<?> tag) {
        return input("#" + tag.key());
    }

    public StonecuttingRecipeDefinition input(Material material) {
        return input(material.key().toString());
    }

    public StonecuttingRecipeDefinition result(String result) {
        this.result = result;
        return this;
    }

    @Override
    public Object toDict() {
        final HashMap<String, Object> dict = new HashMap<>();
        dict.put("Input", this.input);
        dict.put("Result", this.result);
        dict.put("Type", "stonecutting");
        return dict;
    }

    @Override
    public RecipeDefinition fromDict(Object dict) {
        if (!(dict instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                "Invalid stonecutting recipe dictionary: Argument must be a Map<String, Object>!"
            );
        }
        final var dictMap = (Map<?, ?>) dict;
        final Object inputObj = dictMap.containsKey("Input") ? dictMap.get("Input") : dictMap.get("input");
        if (inputObj instanceof String input) {
            this.input = input;
        } else {
            throw new IllegalArgumentException("Invalid stonecutting recipe dictionary: input must be a string");
        }

        final Object resultObj = dictMap.containsKey("Result") ? dictMap.get("Result") : dictMap.get("result");
        if (resultObj instanceof String result) {
            this.result = result;
        } else {
            throw new IllegalArgumentException("Invalid stonecutting recipe dictionary: result must be a string");
        }

        return this;
    }

    @Override
    public Recipe toRecipe(NamespacedKey baseKey) {
        final var out = ItemUtil.itemstackFromString(this.result).getLeft();
        final var in = RecipeDefinition.recipeChoice(input);
        return new StonecuttingRecipe(key(baseKey), out, in);
    }
}
