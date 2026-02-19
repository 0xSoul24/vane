package org.oddlama.vane.trifles;

import static org.oddlama.vane.util.ItemUtil.ItemStackComparator;

import java.util.Arrays;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Barrel;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.data.CooldownData;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.StorageUtil;

public class ChestSorter extends Listener<Trifles> {

    public static final NamespacedKey LAST_SORT_TIME = StorageUtil.namespacedKey("vane_trifles", "last_sort_time");

    @ConfigLong(def = 1000, min = 0, desc = "Chest sorting cooldown in milliseconds.")
    public long configCooldown;

    private CooldownData cooldownData = null;

    @ConfigInt(
        def = 1,
        min = 0,
        max = 16,
        desc = "Chest sorting radius in X-direction from the button (left-right when looking at the button). A radius of 0 means a column of the block including the button. It is advised to NEVER set the three radius values to more than THREE (3), as sorting a huge area of chests can lead to SEVERE lag! Ideally always keep the Z-radius set to 0 or 1, while only adjusting X and Y. You've been warned."
    )
    public int configRadiusX;

    @ConfigInt(
        def = 1,
        min = 0,
        max = 16,
        desc = "Chest sorting radius in Y-direction from the button (up-down when looking at the button - this can be a horizontal direction if the button is on the ground). A radius of 0 means a column of the block including the button. It is advised to NEVER set the three radius values to more than THREE (3), as sorting a huge area of chests can lead to SEVERE lag! Ideally always keep the Z-radius set to 0 or 1, while only adjusting X and Y. You've been warned."
    )
    public int configRadiusY;

    @ConfigInt(
        def = 1,
        min = 0,
        max = 16,
        desc = "Chest sorting radius in Z-direction from the button (into/out-of the attached block). A radius of 0 means a column of the block including the button. It is advised to NEVER set the three radius values to more than THREE (3), as sorting a huge area of chests can lead to SEVERE lag! Ideally always keep the Z-radius set to 0 or 1, while only adjusting X and Y. You've been warned."
    )
    public int configRadiusZ;

    public ChestSorter(Context<Trifles> context) {
        super(context.group("ChestSorting", "Enables chest sorting when a nearby button is pressed."));
    }

    @Override
    protected void onConfigChange() {
        super.onConfigChange();
        this.cooldownData = new CooldownData(LAST_SORT_TIME, configCooldown);
    }

    private void sortInventory(final Inventory inventory) {
        // Find number of non-null item stacks
        final var savedContents = inventory.getStorageContents();
        int nonNull = 0;
        for (final var i : savedContents) {
            if (i != null) {
                ++nonNull;
            }
        }

        // Make a new array without null items
        final var savedContentsCondensed = new ItemStack[nonNull];
        int cur = 0;
        for (final var i : savedContents) {
            if (i != null) {
                savedContentsCondensed[cur++] = i.clone();
            }
        }

        // Clear and add all items again to stack them. Restore saved contents on failure.
        try {
            inventory.clear();
            final var leftovers = inventory.addItem(savedContentsCondensed);
            if (leftovers.size() != 0) {
                // Abort! Something went totally wrong!
                inventory.setStorageContents(savedContentsCondensed);
                getModule().log.warning("Sorting inventory " + inventory + " produced leftovers!");
            }
        } catch (Exception e) {
            inventory.setStorageContents(savedContentsCondensed);
            throw e;
        }

        // Sort
        final var contents = inventory.getStorageContents();
        Arrays.sort(contents, new ItemStackComparator());
        inventory.setStorageContents(contents);
    }

    private void sortContainer(final Container container) {
        // Check cooldown
        if (!cooldownData.checkOrUpdateCooldown(container)) {
            return;
        }

        sortInventory(container.getInventory());
    }

    private void sortChest(final Chest chest) {
        final var inventory = chest.getInventory();

        // Get persistent data
        final Chest persistentChest;
        if (inventory instanceof DoubleChestInventory) {
            final var leftSide = (((DoubleChestInventory) inventory).getLeftSide()).getHolder();
            if (!(leftSide instanceof Chest)) {
                return;
            }
            persistentChest = (Chest) leftSide;
        } else {
            persistentChest = chest;
        }

        // Check cooldown
        if (!cooldownData.checkOrUpdateCooldown(persistentChest)) {
            return;
        }

        if (persistentChest != chest) {
            // Save the left side block state if we are the right side
            persistentChest.update(true, false);
        }

        sortInventory(inventory);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false) // ignoreCancelled = false to catch right-click-air events
    public void onPlayerRightClick(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Require the action to be block usage
        if (event.useInteractedBlock() != Event.Result.ALLOW) {
            return;
        }

        // Require the clicked block to be a button
        final var rootBlock = event.getClickedBlock();
        if (!Tag.BUTTONS.isTagged(rootBlock.getType())) {
            return;
        }

        final var buttonData = rootBlock.getState().getBlockData();
        final var facing = ((Directional) buttonData).getFacing();
        final var face = ((FaceAttachable) buttonData).getAttachedFace();

        int rx = 0;
        int ry;
        int rz = 0;

        // Determine relative radius rx, ry, rz as seen from the button.
        if (face == FaceAttachable.AttachedFace.WALL) {
            ry = configRadiusY;
            switch (facing) {
                case NORTH:
                case SOUTH:
                    rx = configRadiusX;
                    rz = configRadiusZ;
                    break;
                case EAST:
                case WEST:
                    rx = configRadiusZ;
                    rz = configRadiusX;
                    break;
                default:
                    break;
            }
        } else {
            ry = configRadiusZ;
            switch (facing) {
                case NORTH:
                case SOUTH:
                    rx = configRadiusX;
                    rz = configRadiusY;
                    break;
                case EAST:
                case WEST:
                    rx = configRadiusY;
                    rz = configRadiusX;
                    break;
                default:
                    break;
            }
        }

        // Find chests in configured radius and sort them.
        for (int x = -rx; x <= rx; ++x) {
            for (int y = -ry; y <= ry; ++y) {
                for (int z = -rz; z <= rz; ++z) {
                    final var block = rootBlock.getRelative(x, y, z);
                    final var state = block.getState();
                    if (state instanceof Chest) {
                        sortChest((Chest) state);
                    } else if (state instanceof Barrel) {
                        sortContainer((Barrel) state);
                    }
                }
            }
        }
    }
}
