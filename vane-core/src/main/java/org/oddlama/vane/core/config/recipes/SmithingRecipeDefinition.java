package org.oddlama.vane.core.config.recipes;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.oddlama.vane.util.ItemUtil;

public class SmithingRecipeDefinition extends RecipeDefinition {

    private String base = null;
    private String addition = null;
    private boolean copyNbt = false;
    private String result = null;

    public SmithingRecipeDefinition(String name) {
        super(name);
    }

    public SmithingRecipeDefinition base(String base) {
        this.base = base;
        return this;
    }

    public SmithingRecipeDefinition base(final Tag<?> tag) {
        return base("#" + tag.key());
    }

    public SmithingRecipeDefinition base(Material material) {
        return base(material.key().toString());
    }

    public SmithingRecipeDefinition addition(String addition) {
        this.addition = addition;
        return this;
    }

    public SmithingRecipeDefinition copyNbt(boolean copyNbt) {
        this.copyNbt = copyNbt;
        return this;
    }

    public SmithingRecipeDefinition addition(final Tag<?> tag) {
        return addition("#" + tag.key());
    }

    public SmithingRecipeDefinition addition(Material material) {
        return addition(material.key().toString());
    }

    public SmithingRecipeDefinition result(String result) {
        this.result = result;
        return this;
    }

    @Override
    public Object toDict() {
        final HashMap<String, Object> dict = new HashMap<>();
        dict.put("Base", this.base);
        dict.put("Addition", this.addition);
        dict.put("CopyNbt", this.copyNbt);
        dict.put("Result", this.result);
        dict.put("Type", "smithing");
        return dict;
    }

    @Override
    public RecipeDefinition fromDict(Object dict) {
        if (!(dict instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                "Invalid smithing recipe dictionary: Argument must be a Map<String, Object>!"
            );
        }
        final var dictMap = (Map<?, ?>) dict;
        final Object baseObj = dictMap.containsKey("Base") ? dictMap.get("Base") : dictMap.get("base");
        if (baseObj instanceof String base) {
            this.base = base;
        } else {
            throw new IllegalArgumentException("Invalid smithing recipe dictionary: base must be a string");
        }

        final Object additionObj = dictMap.containsKey("Addition") ? dictMap.get("Addition") : dictMap.get("addition");
        if (additionObj instanceof String addition) {
            this.addition = addition;
        } else {
            throw new IllegalArgumentException("Invalid smithing recipe dictionary: addition must be a string");
        }

        final Object copyNbtObj = dictMap.containsKey("CopyNbt") ? dictMap.get("CopyNbt") : dictMap.get("copyNbt");
        if (copyNbtObj instanceof Boolean copyNbt) {
            this.copyNbt = copyNbt;
        } else {
            throw new IllegalArgumentException("Invalid smithing recipe dictionary: copyNbt must be a bool");
        }

        final Object resultObj = dictMap.containsKey("Result") ? dictMap.get("Result") : dictMap.get("result");
        if (resultObj instanceof String result) {
            this.result = result;
        } else {
            throw new IllegalArgumentException("Invalid smithing recipe dictionary: result must be a string");
        }

        return this;
    }

    @Override
    public Recipe toRecipe(NamespacedKey baseKey) {
        return new SmithingTransformRecipe(
            key(baseKey),
            ItemUtil.itemstackFromString(this.result).getLeft(),
            new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
            RecipeDefinition.recipeChoice(base),
            RecipeDefinition.recipeChoice(addition),
                copyNbt
        );
    }
}
