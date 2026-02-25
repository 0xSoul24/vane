package org.oddlama.vane.util

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry

object MaterialUtil {
    @JvmStatic
    fun materialFrom(key: NamespacedKey): Material? {
        return Registry.MATERIAL.get(key)
    }

    @JvmStatic
    fun isSeededPlant(type: Material): Boolean {
        return when (type) {
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART -> true
            else -> false
        }
    }

    @JvmStatic
    fun seedFor(plantType: Material): Material? {
        return when (plantType) {
            Material.WHEAT -> Material.WHEAT_SEEDS
            Material.CARROTS -> Material.CARROT
            Material.POTATOES -> Material.POTATO
            Material.BEETROOTS -> Material.BEETROOT_SEEDS
            Material.NETHER_WART -> Material.NETHER_WART
            else -> null
        }
    }

    @JvmStatic
    fun farmlandFor(seedType: Material): Material? {
        return when (seedType) {
            Material.WHEAT_SEEDS, Material.CARROT, Material.POTATO, Material.BEETROOT_SEEDS -> Material.FARMLAND
            Material.NETHER_WART -> Material.SOUL_SAND
            else -> null
        }
    }

    fun isReplaceableGrass(type: Material): Boolean {
        return when (type) {
            Material.TALL_GRASS, Material.SHORT_GRASS -> true
            else -> false
        }
    }

    @JvmStatic
    fun isTillable(type: Material): Boolean {
        return when (type) {
            Material.DIRT, Material.GRASS_BLOCK, Material.DIRT_PATH -> true
            else -> false
        }
    }
}
