package org.oddlama.vane.util

import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.util.BlockUtil.dropNaturally


object PlayerUtil {
    @JvmStatic
    fun applyElytraBoost(player: Player, factor: Double) {
        val v = player.location.getDirection()
        v.normalize()
        v.multiply(factor)

        // Set velocity, play sound
        player.velocity = player.velocity.add(v)
        player
            .world
            .playSound(player.location, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.4f, 2.0f)
    }

    @JvmStatic
    fun removeOneItemFromHand(player: Player, hand: EquipmentSlot) {
        val item = player.equipment.getItem(hand)
        if (item.amount == 1) {
            player.inventory.setItem(hand, null)
        } else {
            item.amount -= 1
            player.inventory.setItem(hand, item)
        }
    }

    // ItemStack amounts are discarded, only the mapped value counts.
    // CAUTION: There must not be duplicate item keys that could stack.
    @JvmStatic
    fun hasItems(player: Player, items: MutableMap<ItemStack?, Int>): Boolean {
        if (player.gameMode == GameMode.CREATIVE) {
            return true
        }

        val inventory = player.inventory
        for (e in items.entries) {
            val item = e.key!!.clone()
            item.amount = 1
            val amount = e.value
            if (!inventory.containsAtLeast(item, amount)) {
                return false
            }
        }

        return true
    }

    @JvmStatic
    fun takeItems(player: Player, item: ItemStack): Boolean {
        val map = HashMap<ItemStack?, Int>()
        map[item] = item.amount
        return takeItems(player, map)
    }

    @JvmStatic
    fun takeItems(player: Player, items: MutableMap<ItemStack?, Int>): Boolean {
        if (player.gameMode == GameMode.CREATIVE) {
            return true
        }

        if (!hasItems(player, items)) {
            return false
        }

        val inventory = player.inventory
        val stacks = items.entries.flatMap { e -> createLawfulStacks(e.key!!, e.value).toList() }

        val leftovers = inventory.removeItem(*stacks.toTypedArray())
        if (!leftovers.isEmpty()) {
            Bukkit.getLogger()
                .warning(
                    "[vane] Unexpected leftovers while removing the following items from a player's inventory: " +
                            stacks
                )
            for (l in leftovers.entries) {
                Bukkit.getLogger().warning("[vane] Leftover: " + l.key + ", amount: " + l.value)
            }
            return false
        }

        return true
    }

    @JvmStatic
    fun giveItem(player: Player, item: ItemStack?) {
        if (item != null) giveItems(player, arrayOf(item))
    }

    // Ignores item.getAmount().
    fun createLawfulStacks(item: ItemStack, amount: Int): Array<ItemStack> {
        val stacks = (item.maxStackSize - 1 + amount) / item.maxStackSize
        val leftover = amount % item.maxStackSize
        if (stacks < 1) {
            return emptyArray()
        }

        val items = Array(stacks) {
            val clone = item.clone()
            clone.amount = item.maxStackSize
            clone
        }
        if (leftover != 0) {
            items[stacks - 1].amount = leftover
        }

        return items
    }

    @JvmStatic
    fun giveItems(player: Player, item: ItemStack, amount: Int) {
        giveItems(player, createLawfulStacks(item, amount))
    }

    fun giveItems(player: Player, items: Array<ItemStack>) {
        val leftovers = player.inventory.addItem(*items)
        for (item in leftovers.values) {
            player.location.getWorld().dropItem(player.location, item).pickupDelay = 0
        }
    }

    @JvmStatic
    fun tillBlock(player: Player, block: Block): Boolean {
        // Create block break event for block to till and check if it gets canceled
        val breakEvent = BlockBreakEvent(block, player)
        Bukkit.getPluginManager().callEvent(breakEvent)
        if (breakEvent.isCancelled) {
            return false
        }

        // Till block
        block.type = Material.FARMLAND

        // Play sound
        player.world.playSound(player.location, Sound.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 1.0f)
        return true
    }

    @JvmStatic
    fun seedBlock(
        player: Player,
        usedItem: ItemStack,
        block: Block,
        plantType: Material,
        seedType: Material
    ): Boolean {
        // Create block place event for seed to place and check if it gets canceled
        val below = block.getRelative(BlockFace.DOWN)
        val placeEvent = BlockPlaceEvent(
            block,
            below.state,
            below,
            usedItem,
            player,
            true,
            EquipmentSlot.HAND
        )
        Bukkit.getPluginManager().callEvent(placeEvent)
        if (placeEvent.isCancelled) {
            return false
        }

        // Remove one seed from inventory if not in creative mode
        if (player.gameMode != GameMode.CREATIVE) {
            val seedstack = ItemStack(seedType, 1)
            if (!player.inventory.containsAtLeast(seedstack, 1)) {
                return false
            }

            player.inventory.removeItem(seedstack)
        }

        // Set block seeded
        block.type = plantType
        val ageable = block.blockData as Ageable
        ageable.age = 0
        block.blockData = ageable

        // Play sound
        player
            .world
            .playSound(
                player.location,
                if (seedType == Material.NETHER_WART) Sound.ITEM_NETHER_WART_PLANT else Sound.ITEM_CROP_PLANT,
                SoundCategory.BLOCKS,
                1.0f,
                1.0f
            )
        return true
    }

    @JvmStatic
    fun harvestPlant(player: Player, block: Block): Boolean {
        val drops: Array<ItemStack>?
        when (block.type) {
            Material.WHEAT -> drops = arrayOf(ItemStack(Material.WHEAT, 1 + (Math.random() * 2.5).toInt()))
            Material.CARROTS -> drops =
                arrayOf(ItemStack(Material.CARROT, 1 + (Math.random() * 2.5).toInt()))

            Material.POTATOES -> drops =
                arrayOf(ItemStack(Material.POTATO, 1 + (Math.random() * 2.5).toInt()))

            Material.BEETROOTS -> drops =
                arrayOf(ItemStack(Material.BEETROOT, 1 + (Math.random() * 2.5).toInt()))

            Material.NETHER_WART -> drops =
                arrayOf(ItemStack(Material.NETHER_WART, 1 + (Math.random() * 2.5).toInt()))

            else -> return false
        }

        if (block.blockData !is Ageable) {
            return false
        }

        // Only harvest fully grown plants
        val ageable = block.blockData as Ageable
        if (ageable.age != ageable.maximumAge) {
            return false
        }

        // Create a block break event for block to harvest and check if it gets canceled
        val breakEvent = BlockBreakEvent(block, player)
        Bukkit.getPluginManager().callEvent(breakEvent)
        if (breakEvent.isCancelled) {
            return false
        }

        // Reset crop state
        ageable.age = 0
        block.blockData = ageable

        // Drop items
        for (drop in drops) {
            dropNaturally(block, drop)
        }

        return true
    }

    @JvmStatic
    fun swingArm(player: Player, hand: EquipmentSlot) {
        when (hand) {
            EquipmentSlot.HAND -> player.swingMainHand()
            EquipmentSlot.OFF_HAND -> player.swingOffHand()
            else -> {}
        }
    }
}
