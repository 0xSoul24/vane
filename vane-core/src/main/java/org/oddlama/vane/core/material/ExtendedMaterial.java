package org.oddlama.vane.core.material;

import static org.oddlama.vane.util.MaterialUtil.materialFrom;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.item.api.CustomItem;

public class ExtendedMaterial {

    private NamespacedKey key;
    private Material material;
    private HeadMaterial headMaterial;

    private ExtendedMaterial(final NamespacedKey key) {
        this.key = key;
        this.material = materialFrom(key);
        if (this.material == null) {
            this.headMaterial = HeadMaterialLibrary.from(key);
        } else {
            this.headMaterial = null;
        }
    }

    public NamespacedKey key() {
        return key;
    }

    public boolean isSimpleMaterial() {
        return material != null;
    }

    public static ExtendedMaterial from(final NamespacedKey key) {
        final var mat = new ExtendedMaterial(key);
        if (mat.material == null && mat.headMaterial == null && key.namespace().equals("minecraft")) {
            // If no material was found and the key doesn't suggest a custom item, return null.
            return null;
        }
        return mat;
    }

    public static ExtendedMaterial from(final Material material) {
        return from(material.getKey());
    }

    public static ExtendedMaterial from(final CustomItem customItem) {
        return from(customItem.key());
    }

    public ItemStack item() {
        return item(1);
    }

    public ItemStack item(int amount) {
        if (headMaterial != null) {
            final var item = headMaterial.item();
            item.setAmount(amount);
            return item;
        }
        if (material != null) {
            return new ItemStack(material, amount);
        }

        final var customItem = Core.instance().itemRegistry().get(key);
        if (customItem == null) {
            throw new IllegalStateException(
                "ExtendedMaterial '" + key + "' is neither a classic material, a head nor a custom item!"
            );
        }

        return customItem.newStack();
    }
}
