package org.oddlama.vane.util;

import static org.oddlama.vane.util.BlockUtil.dropNaturally;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerUtil {

    public static void applyElytraBoost(final Player player, double factor) {
        final var v = player.getLocation().getDirection();
        v.normalize();
        v.multiply(factor);

        // Set velocity, play sound
        player.setVelocity(player.getVelocity().add(v));
        player
            .getWorld()
            .playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.4f, 2.0f);
    }

    public static void removeOneItemFromHand(final Player player, final EquipmentSlot hand) {
        final var item = player.getEquipment().getItem(hand);
        if (item.getAmount() == 1) {
            player.getInventory().setItem(hand, null);
        } else {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItem(hand, item);
        }
    }

    // ItemStack amounts are discarded, only the mapped value counts.
    // CAUTION: There must not be duplicate item keys that could stack.
    public static boolean hasItems(final Player player, final Map<ItemStack, Integer> items) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        final var inventory = player.getInventory();
        for (final var e : items.entrySet()) {
            final var item = e.getKey().clone();
            item.setAmount(1);
            final var amount = e.getValue();
            if (!inventory.containsAtLeast(item, amount)) {
                return false;
            }
        }

        return true;
    }

    public static boolean takeItems(final Player player, final ItemStack item) {
        final var map = new HashMap<ItemStack, Integer>();
        map.put(item, item.getAmount());
        return takeItems(player, map);
    }

    public static boolean takeItems(final Player player, final Map<ItemStack, Integer> items) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        if (!hasItems(player, items)) {
            return false;
        }

        final var inventory = player.getInventory();
        final var stacks = new ArrayList<ItemStack>();
        for (final var e : items.entrySet()) {
            stacks.addAll(Arrays.asList(createLawfulStacks(e.getKey(), e.getValue())));
        }

        final var leftovers = inventory.removeItem(stacks.toArray(new ItemStack[0]));
        if (!leftovers.isEmpty()) {
            Bukkit.getLogger()
                .warning(
                    "[vane] Unexpected leftovers while removing the following items from a player's inventory: " +
                    stacks
                );
            for (final var l : leftovers.entrySet()) {
                Bukkit.getLogger().warning("[vane] Leftover: " + l.getKey() + ", amount: " + l.getValue());
            }
            return false;
        }

        return true;
    }

    public static void giveItem(final Player player, final ItemStack item) {
        giveItems(player, new ItemStack[] { item });
    }

    // Ignores item.getAmount().
    public static ItemStack[] createLawfulStacks(final ItemStack item, int amount) {
        final var stacks = (item.getMaxStackSize() - 1 + amount) / item.getMaxStackSize();
        final var leftover = amount % item.getMaxStackSize();
        if (stacks < 1) {
            return new ItemStack[] {};
        }

        final var items = new ItemStack[stacks];
        for (int i = 0; i < stacks; ++i) {
            items[i] = item.clone();
            items[i].setAmount(item.getMaxStackSize());
        }
        if (leftover != 0) {
            items[stacks - 1].setAmount(leftover);
        }

        return items;
    }

    public static void giveItems(final Player player, final ItemStack item, int amount) {
        giveItems(player, createLawfulStacks(item, amount));
    }

    public static void giveItems(final Player player, final ItemStack[] items) {
        final var leftovers = player.getInventory().addItem(items);
        for (final var item : leftovers.values()) {
            player.getLocation().getWorld().dropItem(player.getLocation(), item).setPickupDelay(0);
        }
    }

    public static boolean tillBlock(final Player player, final Block block) {
        // Create block break event for block to till and check if it gets canceled
        final var breakEvent = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) {
            return false;
        }

        // Till block
        block.setType(Material.FARMLAND);

        // Play sound
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    public static boolean seedBlock(
        final Player player,
        final ItemStack usedItem,
        final Block block,
        final Material plantType,
        final Material seedType
    ) {
        // Create block place event for seed to place and check if it gets canceled
        final var below = block.getRelative(BlockFace.DOWN);
        final var placeEvent = new BlockPlaceEvent(
            block,
            below.getState(),
            below,
            usedItem,
            player,
            true,
            EquipmentSlot.HAND
        );
        Bukkit.getPluginManager().callEvent(placeEvent);
        if (placeEvent.isCancelled()) {
            return false;
        }

        // Remove one seed from inventory if not in creative mode
        if (player.getGameMode() != GameMode.CREATIVE) {
            final var seedstack = new ItemStack(seedType, 1);
            if (!player.getInventory().containsAtLeast(seedstack, 1)) {
                return false;
            }

            player.getInventory().removeItem(seedstack);
        }

        // Set block seeded
        block.setType(plantType);
        final var ageable = (Ageable) block.getBlockData();
        ageable.setAge(0);
        block.setBlockData(ageable);

        // Play sound
        player
            .getWorld()
            .playSound(
                player.getLocation(),
                seedType == Material.NETHER_WART ? Sound.ITEM_NETHER_WART_PLANT : Sound.ITEM_CROP_PLANT,
                SoundCategory.BLOCKS,
                1.0f,
                1.0f
            );
        return true;
    }

    public static boolean harvestPlant(final Player player, final Block block) {
        ItemStack[] drops;
        switch (block.getType()) {
            default:
                return false;
            case WHEAT:
                drops = new ItemStack[] { new ItemStack(Material.WHEAT, 1 + (int) (Math.random() * 2.5)) };
                break;
            case CARROTS:
                drops = new ItemStack[] { new ItemStack(Material.CARROT, 1 + (int) (Math.random() * 2.5)) };
                break;
            case POTATOES:
                drops = new ItemStack[] { new ItemStack(Material.POTATO, 1 + (int) (Math.random() * 2.5)) };
                break;
            case BEETROOTS:
                drops = new ItemStack[] { new ItemStack(Material.BEETROOT, 1 + (int) (Math.random() * 2.5)) };
                break;
            case NETHER_WART:
                drops = new ItemStack[] { new ItemStack(Material.NETHER_WART, 1 + (int) (Math.random() * 2.5)) };
                break;
        }

        if (!(block.getBlockData() instanceof Ageable)) {
            return false;
        }

        // Only harvest fully grown plants
        var ageable = (Ageable) block.getBlockData();
        if (ageable.getAge() != ageable.getMaximumAge()) {
            return false;
        }

        // Create a block break event for block to harvest and check if it gets canceled
        final var breakEvent = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) {
            return false;
        }

        // Reset crop state
        ageable.setAge(0);
        block.setBlockData(ageable);

        // Drop items
        for (ItemStack drop : drops) {
            BlockUtil.dropNaturally(block, drop);
        }

        return true;
    }

    public static void swingArm(final Player player, final EquipmentSlot hand) {
        switch (hand) {
            case HAND:
                player.swingMainHand();
                break;
            case OFF_HAND:
                player.swingOffHand();
                break;
        }
    }
}
