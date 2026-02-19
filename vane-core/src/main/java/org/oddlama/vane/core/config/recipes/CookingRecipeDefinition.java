package org.oddlama.vane.core.config.recipes;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmokingRecipe;
import org.oddlama.vane.util.ItemUtil;

public class CookingRecipeDefinition extends RecipeDefinition {

    private String input = null;
    private String result = null;
    private float experience = 0.0f;
    private int cookingTime = 10;
    private String type;

    public CookingRecipeDefinition(String name, String type) {
        super(name);
        this.type = type;
        switch (this.type) {
            case "blasting": // fallthrough
            case "furnace": // fallthrough
            case "campfire": // fallthrough
            case "smoking":
                break;
            default:
                throw new IllegalArgumentException("Invalid cooking recipe: Unknown type '" + this.type + "'");
        }
    }

    public CookingRecipeDefinition input(String input) {
        this.input = input;
        return this;
    }

    public CookingRecipeDefinition input(final Tag<?> tag) {
        return input("#" + tag.key());
    }

    public CookingRecipeDefinition input(Material material) {
        return input(material.key().toString());
    }

    public CookingRecipeDefinition result(String result) {
        this.result = result;
        return this;
    }

    @Override
    public Object toDict() {
        final HashMap<String, Object> dict = new HashMap<>();
        dict.put("CookingTime", this.cookingTime);
        dict.put("Experience", this.experience);
        dict.put("Input", this.input);
        dict.put("Result", this.result);
        dict.put("Type", type);
        return dict;
    }

    @Override
    public RecipeDefinition fromDict(Object dict) {
        if (!(dict instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                "Invalid " + type + " recipe dictionary: Argument must be a Map<String, Object>!"
            );
        }
        final var dictMap = (Map<?, ?>) dict;
        final Object inputObj = dictMap.containsKey("Input") ? dictMap.get("Input") : dictMap.get("input");
        if (inputObj instanceof String input) {
            this.input = input;
        } else {
            throw new IllegalArgumentException("Invalid " + type + " recipe dictionary: input must be a string");
        }

        final Object resultObj = dictMap.containsKey("Result") ? dictMap.get("Result") : dictMap.get("result");
        if (resultObj instanceof String result) {
            this.result = result;
        } else {
            throw new IllegalArgumentException("Invalid " + type + " recipe dictionary: result must be a string");
        }

        final Object experienceObj = dictMap.containsKey("Experience") ? dictMap.get("Experience") : dictMap.get("experience");
        if (experienceObj instanceof Float experience) {
            this.experience = experience;
        } else {
            throw new IllegalArgumentException("Invalid " + type + " recipe dictionary: experience must be a float");
        }

        final Object cookingTimeObj = dictMap.containsKey("CookingTime") ? dictMap.get("CookingTime") : dictMap.get("cookingTime");
        if (cookingTimeObj instanceof Integer cookingTime) {
            this.cookingTime = cookingTime;
        } else {
            throw new IllegalArgumentException("Invalid " + type + " recipe dictionary: cookingTime must be a int");
        }

        return this;
    }

    @Override
    public Recipe toRecipe(NamespacedKey baseKey) {
        final var out = ItemUtil.itemstackFromString(this.result).getLeft();
        final var in = RecipeDefinition.recipeChoice(input);
        switch (this.type) {
            case "blasting":
                return new BlastingRecipe(key(baseKey), out, in, experience, cookingTime);
            case "furnace":
                return new FurnaceRecipe(key(baseKey), out, in, experience, cookingTime);
            case "campfire":
                return new CampfireRecipe(key(baseKey), out, in, experience, cookingTime);
            case "smoking":
                return new SmokingRecipe(key(baseKey), out, in, experience, cookingTime);
            default:
                throw new IllegalArgumentException("Invalid cooking recipe: Unknown type '" + this.type + "'");
        }
    }
}
