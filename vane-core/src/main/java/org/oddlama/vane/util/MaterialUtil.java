package org.oddlama.vane.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

public class MaterialUtil {

    public static Material materialFrom(NamespacedKey key) {
        return Registry.MATERIAL.get(key);
    }

    public static boolean isSeededPlant(Material type) {
        switch (type) {
            default:
                return false;
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case NETHER_WART:
                return true;
        }
    }

    public static Material seedFor(Material plantType) {
        switch (plantType) {
            default:
                return null;
            case WHEAT:
                return Material.WHEAT_SEEDS;
            case CARROTS:
                return Material.CARROT;
            case POTATOES:
                return Material.POTATO;
            case BEETROOTS:
                return Material.BEETROOT_SEEDS;
            case NETHER_WART:
                return Material.NETHER_WART;
        }
    }

    public static Material farmlandFor(Material seedType) {
        switch (seedType) {
            default:
                return null;
            case WHEAT_SEEDS:
            case CARROT:
            case POTATO:
            case BEETROOT_SEEDS:
                return Material.FARMLAND;
            case NETHER_WART:
                return Material.SOUL_SAND;
        }
    }

    public static boolean isReplaceableGrass(Material type) {
        switch (type) {
            default:
                return false;
            case TALL_GRASS:
            case SHORT_GRASS:
                return true;
        }
    }

    public static boolean isTillable(Material type) {
        switch (type) {
            default:
                return false;
            case DIRT:
            case GRASS_BLOCK:
            case DIRT_PATH:
                return true;
        }
    }
}
