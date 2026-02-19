package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.ItemUtil.nameItem;

import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.lang.LangMessageArray;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.lang.TranslatedMessageArray;
import org.oddlama.vane.core.material.ExtendedMaterial;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

public class TranslatedItemStack<T extends Module<T>> extends ModuleComponent<T> {

    @ConfigInt(def = 1, min = 0, desc = "The item stack amount.")
    public int configAmount;

    @ConfigExtendedMaterial(
        def = "minecraft:barrier",
        desc = "The item stack material. Also accepts heads from the head library or from defined custom items."
    )
    public ExtendedMaterial configMaterial;

    @LangMessage
    public TranslatedMessage langName;

    @LangMessageArray
    public TranslatedMessageArray langLore;

    private ExtendedMaterial defMaterial;
    private int defAmount;

    public TranslatedItemStack(
        final Context<T> context,
        final String configNamespace,
        final NamespacedKey defMaterial,
        int defAmount,
        final String desc
    ) {
        this(context, configNamespace, ExtendedMaterial.from(defMaterial), defAmount, desc);
    }

    public TranslatedItemStack(
        final Context<T> context,
        final String configNamespace,
        final Material defMaterial,
        int defAmount,
        final String desc
    ) {
        this(context, configNamespace, ExtendedMaterial.from(defMaterial), defAmount, desc);
    }

    public TranslatedItemStack(
        final Context<T> context,
        final String configNamespace,
        final ExtendedMaterial defMaterial,
        int defAmount,
        final String desc
    ) {
        super(context.namespace(configNamespace, desc));
        this.defMaterial = defMaterial;
        this.defAmount = defAmount;
    }

    public ItemStack item(Object... args) {
        return nameItem(configMaterial.item(configAmount), langName.format(args), langLore.format(args));
    }

    public ItemStack itemTransformLore(Consumer<List<Component>> fLore, Object... args) {
        final var lore = langLore.format(args);
        fLore.accept(lore);
        return nameItem(configMaterial.item(configAmount), langName.format(args), lore);
    }

    public ItemStack itemAmount(int amount, Object... args) {
        return nameItem(configMaterial.item(amount), langName.format(args), langLore.format(args));
    }

    public ItemStack alternative(final ItemStack alternative, Object... args) {
        return nameItem(alternative, langName.format(args), langLore.format(args));
    }

    public ExtendedMaterial configMaterialDef() {
        return defMaterial;
    }

    public int configAmountDef() {
        return defAmount;
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
