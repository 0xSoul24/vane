package org.oddlama.vane.regions.region

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.util.PlayerUtil
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow

class RegionSelection(private val regions: Regions) {
    @JvmField
    var primary: Block? = null
    @JvmField
    var secondary: Block? = null

    fun intersectsExisting(): Boolean {
        val extent = extent()
        for (r in regions.allRegions()) {
            val region = r ?: continue
            val rExtent = region.extent() ?: continue
            val rMin = rExtent.min() ?: continue
            if (rMin.world != primary!!.world) {
                continue
            }

            if (extent.intersectsExtent(rExtent)) {
                return true
            }
        }

        return false
    }

    fun price(): Double {
        val dx = 1 + abs(primary!!.x - secondary!!.x)
        val dy = 1 + abs(primary!!.y - secondary!!.y)
        val dz = 1 + abs(primary!!.z - secondary!!.z)
        val cost =
            ((regions.configCostYMultiplicator.pow(dy / 16.0) * regions.configCostXzBase) / 256.0) *
                    dx *
                    dz
        return if (regions.configEconomyAsCurrency) {
            var decimalPlaces = regions.configEconomyDecimalPlaces
            if (decimalPlaces == -1) {
                decimalPlaces = regions.economy?.fractionalDigits() ?: decimalPlaces
            }

            if (decimalPlaces >= 0) {
                BigDecimal(cost).setScale(decimalPlaces, RoundingMode.UP).toDouble()
            } else {
                cost
            }
        } else {
            ceil(cost)
        }
    }

    fun canAfford(player: Player): Boolean {
        val price = price()
        if (price <= 0) {
            return true
        }

        if (regions.configEconomyAsCurrency) {
            return regions.economy?.has(player, price) ?: false
        } else {
            val map: MutableMap<ItemStack?, Int> = HashMap()
            val currency = regions.configCurrency ?: Material.DIAMOND
            map[ItemStack(currency)] = price.toInt()
            return PlayerUtil.hasItems(player, map)
        }
    }

    fun isValid(player: Player): Boolean {
        // Both block sets
        if (primary == null || secondary == null) {
            return false
        }

        // World match
        if (primary!!.world != secondary!!.world) {
            return false
        }

        val dx = 1 + abs(primary!!.x - secondary!!.x)
        val dy = 1 + abs(primary!!.y - secondary!!.y)
        val dz = 1 + abs(primary!!.z - secondary!!.z)

        // min <= extent <= max
        if (dx < regions.configMinRegionExtentX || dy < regions.configMinRegionExtentY || dz < regions.configMinRegionExtentZ || dx > regions.configMaxRegionExtentX || dy > regions.configMaxRegionExtentY || dz > regions.configMaxRegionExtentZ
        ) {
            return false
        }

        // Assert that it doesn't intersect an existing region
        if (intersectsExisting()) {
            return false
        }

        // Check that the player can afford it
        return canAfford(player)
    }

    fun extent(): RegionExtent {
        return RegionExtent(primary!!, secondary!!)
    }
}
