package org.oddlama.vane.regions.region;

import static org.oddlama.vane.util.PlayerUtil.hasItems;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.regions.Regions;

public class RegionSelection {

    private Regions regions;
    public Block primary = null;
    public Block secondary = null;

    public RegionSelection(final Regions regions) {
        this.regions = regions;
    }

    public boolean intersectsExisting() {
        final var extent = extent();
        for (final var r : regions.allRegions()) {
            if (!r.extent().min().getWorld().equals(primary.getWorld())) {
                continue;
            }

            if (extent.intersectsExtent(r.extent())) {
                return true;
            }
        }

        return false;
    }

    public double price() {
        final var dx = 1 + Math.abs(primary.getX() - secondary.getX());
        final var dy = 1 + Math.abs(primary.getY() - secondary.getY());
        final var dz = 1 + Math.abs(primary.getZ() - secondary.getZ());
        final var cost =
            ((Math.pow(regions.configCostYMultiplicator, dy / 16.0) * regions.configCostXzBase) / 256.0) *
            dx *
            dz;
        if (regions.configEconomyAsCurrency) {
            int decimalPlaces = regions.configEconomyDecimalPlaces;
            if (decimalPlaces == -1) {
                decimalPlaces = regions.economy.fractionalDigits();
            }

            if (decimalPlaces >= 0) {
                return new BigDecimal(cost).setScale(decimalPlaces, RoundingMode.UP).doubleValue();
            } else {
                return cost;
            }
        } else {
            return Math.ceil(cost);
        }
    }

    public boolean canAfford(final Player player) {
        final var price = price();
        if (price <= 0) {
            return true;
        }

        if (regions.configEconomyAsCurrency) {
            return regions.economy.has(player, price);
        } else {
            final var map = new HashMap<ItemStack, Integer>();
            map.put(new ItemStack(regions.configCurrency), (int) price);
            return hasItems(player, map);
        }
    }

    public boolean isValid(final Player player) {
        // Both block sets
        if (primary == null || secondary == null) {
            return false;
        }

        // World match
        if (!primary.getWorld().equals(secondary.getWorld())) {
            return false;
        }

        final var dx = 1 + Math.abs(primary.getX() - secondary.getX());
        final var dy = 1 + Math.abs(primary.getY() - secondary.getY());
        final var dz = 1 + Math.abs(primary.getZ() - secondary.getZ());

        // min <= extent <= max
        if (
            dx < regions.configMinRegionExtentX ||
            dy < regions.configMinRegionExtentY ||
            dz < regions.configMinRegionExtentZ ||
            dx > regions.configMaxRegionExtentX ||
            dy > regions.configMaxRegionExtentY ||
            dz > regions.configMaxRegionExtentZ
        ) {
            return false;
        }

        // Assert that it doesn't intersect an existing region
        if (intersectsExisting()) {
            return false;
        }

        // Check that the player can afford it
        return canAfford(player);
    }

    public RegionExtent extent() {
        return new RegionExtent(primary, secondary);
    }
}
