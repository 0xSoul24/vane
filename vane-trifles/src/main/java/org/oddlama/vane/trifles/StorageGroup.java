package org.oddlama.vane.trifles;

import net.kyori.adventure.text.Component;
import org.bukkit.Nameable;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.item.api.CustomItem;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.external.apache.commons.lang3.tuple.Pair;
import org.oddlama.vane.trifles.items.storage.Backpack;
import org.oddlama.vane.trifles.items.storage.Pouch;
import org.oddlama.vane.util.StorageUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageGroup extends Listener<Trifles> {

    public static final NamespacedKey STORAGE_IS_OPEN = StorageUtil.namespacedKey("vane_trifles", "currently_opened_storage");

    private Map<Inventory, Pair<UUID, ItemStack>> openBlockStateInventories = Collections.synchronizedMap(
        new HashMap<Inventory, Pair<UUID, ItemStack>>()
    );

    @LangMessage
    public TranslatedMessage langOpenStackedItem;

    public StorageGroup(Context<Trifles> context) {
        super(context.group("Storage", "Extensions to storage related stuff will be grouped under here."));
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlaceItemInStorageInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Only if no block state inventory is open, else we could delete items by
        // accident
        final var ownerAndItem = openBlockStateInventories.get(event.getInventory());
        if (ownerAndItem != null) {
            return;
        }

        // Put non-storage items in a right-clicked storage item
        if (
            event.getClick() == ClickType.RIGHT &&
            event.getAction() == InventoryAction.SWAP_WITH_CURSOR &&
            isStorageItem(event.getCurrentItem()) &&
            event.getCurrentItem().getAmount() == 1
        ) {
            // Allow putting in any items that are not a storage item.
            if (!isStorageItem(event.getCursor())) {
                final var customItem = getModule().getCore().itemRegistry().get(event.getCurrentItem());

                // Only if the clicked storage item is a custom item
                if (customItem != null) {
                    event
                        .getCurrentItem()
                        .editMeta(BlockStateMeta.class, meta -> {
                            final var blockState = meta.getBlockState();
                            if (blockState instanceof Container container) {
                                final var leftovers = container.getInventory().addItem(event.getCursor());
                                if (leftovers.size() == 0) {
                                    event.setCursor(null);
                                } else {
                                    event.setCursor(leftovers.get(0));
                                }
                                meta.setBlockState(blockState);
                            }
                        });
                }
            }

            // right-clicking a storage item to swap is never "allowed".
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final var ownerAndItem = openBlockStateInventories.get(event.getInventory());
        if (ownerAndItem == null || !ownerAndItem.getLeft().equals(player.getUniqueId())) {
            return;
        }

        var clickedItemIsStorage = isStorageItem(event.getCurrentItem());
        var cursorItemIsStorage = isStorageItem(event.getCursor());
        var clickedInventoryIsPlayerInventory = event.getClickedInventory() == player.getInventory();

        var cancel = false;
        switch (event.getAction()) {
            case DROP_ALL_CURSOR:
            case DROP_ALL_SLOT:
            case DROP_ONE_CURSOR:
            case DROP_ONE_SLOT:
                // Allow dropping storage item because we handle this elsewhere
                cancel = false;
                break;
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
            case SWAP_WITH_CURSOR:
                // Only deny placing storage items in storage inventory
                cancel = cursorItemIsStorage && !clickedInventoryIsPlayerInventory;
                break;
            case MOVE_TO_OTHER_INVENTORY:
                // Only deny moving storage item from player inventory to storage inventory
                cancel = clickedItemIsStorage && clickedInventoryIsPlayerInventory;
                break;
            case PICKUP_ALL:
            case PICKUP_HALF:
            case PICKUP_ONE:
            case PICKUP_SOME:
                // Always allow pickup
                cancel = false;
                break;
            default:
                // Restrictive default prevents moving of any storage items
                cancel = clickedItemIsStorage || cursorItemIsStorage;
                break;
        }

        switch (event.getClick()) {
            case NUMBER_KEY:
                // Deny swapping storage items with number keys into storage inventory, but allow in player inventory
                var swappedItemIsStorage = isStorageItem(player.getInventory().getItem(event.getHotbarButton()));
                cancel = (swappedItemIsStorage || clickedItemIsStorage) && !clickedInventoryIsPlayerInventory;
                break;
        }

        if (cancel) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        if (!isStorageItem(event.getItemDrop().getItemStack())) {
            return;
        }

        // Close the inventory if the player drops the currently open storage item
        var storageItemIsOpenState = isCurrentlyOpen(event.getItemDrop().getItemStack());
        if (storageItemIsOpenState) {
            var isKnownCustomInventory = openBlockStateInventories.containsKey(event.getPlayer().getOpenInventory().getTopInventory());
            if (isKnownCustomInventory) {
                event.getPlayer().closeInventory(InventoryCloseEvent.Reason.CANT_USE);
            } else {
                // Item shouldn't be tagged as open if a custom inventory is not open, fix open tag
                event.getItemDrop().getItemStack().editMeta(meta -> {
                    meta.getPersistentDataContainer().set(STORAGE_IS_OPEN, PersistentDataType.BOOLEAN, false);
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!isStorageItem(event.getItem().getItemStack())) {
            return;
        }

        // Ensure bugged/old storage items are set to closed when picked up just in case
        event.getItem().getItemStack().editMeta(meta -> {
            meta.getPersistentDataContainer().set(STORAGE_IS_OPEN, PersistentDataType.BOOLEAN, false);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final var ownerAndItem = openBlockStateInventories.get(event.getInventory());
        if (ownerAndItem == null || !ownerAndItem.getLeft().equals(player.getUniqueId())) {
            return;
        }

        // Prevent putting storage items in other storage items
        for (final var itemStack : event.getNewItems().values()) {
            if (isStorageItem(itemStack)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void saveAfterClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final var ownerAndItem = openBlockStateInventories.get(event.getInventory());
        if (ownerAndItem == null || !ownerAndItem.getLeft().equals(player.getUniqueId())) {
            return;
        }

        updateStorageItem(ownerAndItem.getRight(), event.getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void saveAfterDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final var ownerAndItem = openBlockStateInventories.get(event.getInventory());
        if (ownerAndItem == null || !ownerAndItem.getLeft().equals(player.getUniqueId())) {
            return;
        }

        updateStorageItem(ownerAndItem.getRight(), event.getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void saveAfterClose(InventoryCloseEvent event) {
        final var ownerAndItem = openBlockStateInventories.get(event.getInventory());
        if (ownerAndItem == null || !ownerAndItem.getLeft().equals(event.getPlayer().getUniqueId())) {
            return;
        }

        // Set the storage item to closed
        ownerAndItem.getRight().editMeta(meta -> {
            meta.getPersistentDataContainer().set(STORAGE_IS_OPEN, PersistentDataType.BOOLEAN, false);
        });
        updateStorageItem(ownerAndItem.getRight(), event.getInventory());
        openBlockStateInventories.remove(event.getInventory());
    }

    private boolean isStorageItem(@Nullable ItemStack item) {
        if (item == null) {
            return false;
        }

        var customItem = getModule().getCore().itemRegistry().get(item);
        if (customItem != null && (customItem instanceof Backpack || customItem instanceof Pouch)) {
            return true;
        }

        // Any item that has a container block state as the meta is a container to us.
        // If the item has no meta (i.e., is empty), it doesn't count.
        return item.getItemMeta() instanceof BlockStateMeta meta && meta.getBlockState() instanceof ShulkerBox;
    }

    private boolean isCurrentlyOpen(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
            return Boolean.TRUE.equals(item.getPersistentDataContainer().get(STORAGE_IS_OPEN, PersistentDataType.BOOLEAN));
    }

    private void updateStorageItem(@NotNull ItemStack item, @NotNull Inventory inventory) {
        // Find the correct storage item if it was moved from inventory slot and is no longer valid
        if (item.getType().isAir() && inventory.getHolder() instanceof Player player) {
            // Check cursor item first
            var cursorItem = player.getOpenInventory().getCursor();
            if (cursorItem.hasItemMeta() && isCurrentlyOpen(cursorItem)) {
                item = cursorItem; // Found the storage item that is currently open
                openBlockStateInventories.put(inventory, Pair.of(player.getUniqueId(), item)); // Update Map
            } else { // else check inventory slots
                for (ItemStack checkedItem : player.getInventory().getContents()) {
                    if (checkedItem == null || !checkedItem.hasItemMeta()) {
                        continue;
                    }
                    if (isCurrentlyOpen(checkedItem)) {
                        item = checkedItem; // Found the storage item that is currently open
                        openBlockStateInventories.put(inventory, Pair.of(player.getUniqueId(), item)); // Update Map
                        break;
                    }
                }
            }
        }
        item.editMeta(BlockStateMeta.class, meta -> {
            final var blockState = meta.getBlockState();
            if (blockState instanceof Container container) {
                container.getInventory().setContents(inventory.getContents());
                meta.setBlockState(blockState);
            }
        });
    }

    public boolean openBlockStateInventory(@NotNull final Player player, @NotNull ItemStack item) {
        // Require correct block state meta
        if (
            !(item.getItemMeta() instanceof BlockStateMeta meta) ||
            !(meta.getBlockState() instanceof Container container)
        ) {
            return false;
        }

        // Only if the stack size is 1.
        if (item.getAmount() != 1) {
            getModule().storageGroup.langOpenStackedItem.sendActionBar(player);
            return false;
        }

        // Transfer item name to block-state
        Component name = Component.text("");
        if (meta.getBlockState() instanceof Nameable nameable) {
            name = meta.hasDisplayName() ? meta.displayName() : meta.hasItemName() ? meta.itemName() : null;

            // if the item has neither custom name nor item name (an old item with custom
            // name reset), get the name from registry if possible
            if (name == null) {
                CustomItem customItem = getModule().getCore().itemRegistry().get(item);
                name = customItem != null ? customItem.displayName() : Component.text("");
            }
            nameable.customName(name);
        }

        // Create transient inventory
        final var transientInventory = getModule()
            .getServer()
            .createInventory(player, container.getInventory().getType(), name);
        transientInventory.setContents(container.getInventory().getContents());

        // Open inventory
        openBlockStateInventories.put(transientInventory, Pair.of(player.getUniqueId(), item));
        player.openInventory(transientInventory);
        return true;
    }
}
