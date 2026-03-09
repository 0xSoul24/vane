package org.oddlama.vane.trifles

import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.block.Barrel
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.data.Directional
import org.bukkit.block.data.FaceAttachable
import org.bukkit.block.data.FaceAttachable.AttachedFace
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.DoubleChestInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.data.CooldownData
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.StorageUtil
import java.util.*

class ChestSorter(context: Context<Trifles?>) :
    Listener<Trifles?>(context.group("ChestSorting", "Enables chest sorting when a nearby button is pressed.")) {
    @ConfigLong(def = 1000, min = 0, desc = "Chest sorting cooldown in milliseconds.")
    var configCooldown: Long = 0

    private var cooldownData: CooldownData? = null

    @ConfigInt(
        def = 1,
        min = 0,
        max = 16,
        desc = "Chest sorting radius in X-direction from the button (left-right when looking at the button). A radius of 0 means a column of the block including the button. It is advised to NEVER set the three radius values to more than THREE (3), as sorting a huge area of chests can lead to SEVERE lag! Ideally always keep the Z-radius set to 0 or 1, while only adjusting X and Y. You've been warned."
    )
    var configRadiusX: Int = 0

    @ConfigInt(
        def = 1,
        min = 0,
        max = 16,
        desc = "Chest sorting radius in Y-direction from the button (up-down when looking at the button - this can be a horizontal direction if the button is on the ground). A radius of 0 means a column of the block including the button. It is advised to NEVER set the three radius values to more than THREE (3), as sorting a huge area of chests can lead to SEVERE lag! Ideally always keep the Z-radius set to 0 or 1, while only adjusting X and Y. You've been warned."
    )
    var configRadiusY: Int = 0

    @ConfigInt(
        def = 1,
        min = 0,
        max = 16,
        desc = "Chest sorting radius in Z-direction from the button (into/out-of the attached block). A radius of 0 means a column of the block including the button. It is advised to NEVER set the three radius values to more than THREE (3), as sorting a huge area of chests can lead to SEVERE lag! Ideally always keep the Z-radius set to 0 or 1, while only adjusting X and Y. You've been warned."
    )
    var configRadiusZ: Int = 0

    override fun onConfigChange() {
        super.onConfigChange()
        this.cooldownData = CooldownData(LAST_SORT_TIME, configCooldown)
    }

    private fun sortInventory(inventory: Inventory) {
        // Find number of non-null item stacks
        val savedContents = inventory.storageContents
        var nonNull = 0
        for (i in savedContents) {
            if (i != null) {
                ++nonNull
            }
        }

        // Make a new list without null items
        val savedContentsCondensedList: MutableList<ItemStack> = ArrayList(nonNull)
        for (i in savedContents) {
            if (i != null) {
                savedContentsCondensedList.add(i.clone())
            }
        }

        // Clear and add all items again to stack them. Restore saved contents on failure.
        try {
            inventory.clear()
            val leftovers = inventory.addItem(*savedContentsCondensedList.toTypedArray())
            if (leftovers.isNotEmpty()) {
                // Abort! Something went totally wrong!
                inventory.storageContents = savedContents
                module!!.log.warning("Sorting inventory $inventory produced leftovers!")
            }
        } catch (e: Exception) {
            inventory.storageContents = savedContents
            throw e
        }

        // Sort
        val contents = inventory.storageContents
        Arrays.sort<ItemStack?>(contents, ItemUtil.ItemStackComparator())
        inventory.storageContents = contents
    }

    private fun sortContainer(container: Container) {
        // Check cooldown
        if (!cooldownData!!.checkOrUpdateCooldown(container)) {
            return
        }

        sortInventory(container.inventory)
    }

    private fun sortChest(chest: Chest) {
        val inventory = chest.inventory

        // Get persistent data
        val persistentChest: Chest
        if (inventory is DoubleChestInventory) {
            val leftSide = (inventory.leftSide).holder
            if (leftSide !is Chest) {
                return
            }
            persistentChest = leftSide
        } else {
            persistentChest = chest
        }

        // Check cooldown
        if (!cooldownData!!.checkOrUpdateCooldown(persistentChest)) {
            return
        }

        if (persistentChest !== chest) {
            // Save the left side block state if we are the right side
            persistentChest.update(true, false)
        }

        sortInventory(inventory)
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = false
    ) // ignoreCancelled = false to catch right-click-air events
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        // Require the action to be block usage
        if (event.useInteractedBlock() != Event.Result.ALLOW) {
            return
        }

        // Require the clicked block to be a button
        val rootBlock = event.clickedBlock
        if (!Tag.BUTTONS.isTagged(rootBlock!!.type)) {
            return
        }

        val buttonData = rootBlock.state.blockData
        val facing = (buttonData as Directional).facing
        val face = (buttonData as FaceAttachable).attachedFace

        var rx = 0
        val ry: Int
        var rz = 0

        // Determine relative radius rx, ry, rz as seen from the button.
        if (face == AttachedFace.WALL) {
            ry = configRadiusY
            when (facing) {
                BlockFace.NORTH, BlockFace.SOUTH -> {
                    rx = configRadiusX
                    rz = configRadiusZ
                }

                BlockFace.EAST, BlockFace.WEST -> {
                    rx = configRadiusZ
                    rz = configRadiusX
                }

                else -> {}
            }
        } else {
            ry = configRadiusZ
            when (facing) {
                BlockFace.NORTH, BlockFace.SOUTH -> {
                    rx = configRadiusX
                    rz = configRadiusY
                }

                BlockFace.EAST, BlockFace.WEST -> {
                    rx = configRadiusY
                    rz = configRadiusX
                }

                else -> {}
            }
        }

        // Find chests in configured radius and sort them.
        for (x in -rx..rx) {
            for (y in -ry..ry) {
                for (z in -rz..rz) {
                    val block = rootBlock.getRelative(x, y, z)
                    val state = block.state
                    if (state is Chest) {
                        sortChest(state)
                    } else if (state is Barrel) {
                        sortContainer(state)
                    }
                }
            }
        }
    }

    companion object {
        val LAST_SORT_TIME: NamespacedKey = StorageUtil.namespacedKey("vane_trifles", "last_sort_time")
    }
}
