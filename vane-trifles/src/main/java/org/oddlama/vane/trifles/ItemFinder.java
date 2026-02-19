package org.oddlama.vane.trifles;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

import static org.oddlama.vane.trifles.ItemFinderPacketUtils.indicateChestMatch;
import static org.oddlama.vane.trifles.ItemFinderPacketUtils.indicateContainerMatch;
import static org.oddlama.vane.trifles.ItemFinderPacketUtils.indicateEntityMatch;

public class ItemFinder extends Listener<Trifles> {
    @ConfigInt(
        def = 2,
        min = 1,
        max = 10,
        desc = "The radius of chunks in which containers (and possibly entities) are checked for matching items."
    )
    public int configRadius;

    @ConfigBoolean(def = true, desc = "Also search entities such as players, mobs, minecarts, ...")
    public boolean configSearchEntities;

    @ConfigBoolean(
        def = false,
        desc = "Only allow players to use the shift+rightclick shortcut when they have the shortcut permission `vane.trifles.use_item_find_shortcut`."
    )
    public boolean configRequirePermission;

    private static final int FALLBACK_TASK_INTERVAL = 15;

    // This permission allows players to use the shift+rightclick.
    public final Permission useItemFindShortcutPermission;

    public ItemFinder(Context<Trifles> context) {
        super(
            context.group(
                "ItemFinder",
                "Enables players to search for items in nearby containers by either shift-right-clicking a similar item in their inventory or by using the `/finditem <item>` command."
            )
        );
        // Register admin permission
        useItemFindShortcutPermission = new Permission(
            "vane." + getModule().getAnnotationName() + ".use_item_find_shortcut",
            "Allows a player to use shfit+rightclick to search for items if the require_permission config is set",
            PermissionDefault.FALSE
        );
        getModule().registerPermission(useItemFindShortcutPermission);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerClickInventory(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (configRequirePermission && !player.hasPermission(useItemFindShortcutPermission)) {
            return;
        }

        final var item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // Shift-rightclick
        if (
            !(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getClick() == ClickType.SHIFT_RIGHT)
        ) {
            return;
        }

        event.setCancelled(true);
        if (findItem(player, item.getType())) {
            getModule().scheduleNextTick(player::closeInventory);
        }
    }

    private boolean isContainer(final Block block) {
        return block.getState() instanceof Container;
    }

    private void fallbackIndicateMatch(@NotNull Player player, @NotNull Location location) {
        player.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, location, 130, 0.4, 0.0, 0.0, 0.0);
        player.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, location, 130, 0.0, 0.4, 0.0, 0.0);
        player.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, location, 130, 0.0, 0.0, 0.4, 0.0);
        player.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, location, 70, 0.2, 0.2, 0.2, 0.0);
    }

    public boolean findItem(@NotNull final Player player, @NotNull final Material material) {
        // Find chests in configured radius and sort them.
        boolean anyFound = false;
        final var world = player.getWorld();
        final var originChunk = player.getChunk();
        final var packetEventsEnabled = getModule().packetEventsEnabled;
        for (int cx = originChunk.getX() - configRadius; cx <= originChunk.getX() + configRadius; ++cx) {
            for (int cz = originChunk.getZ() - configRadius; cz <= originChunk.getZ() + configRadius; ++cz) {
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                final var chunk = world.getChunkAt(cx, cz);
                for (final var tileEntity : chunk.getTileEntities(this::isContainer, false)) {
                    if (tileEntity instanceof Container container) {
                        if (container.getInventory().contains(material)) {
                            if (container.getType() == Material.CHEST) {
                                if (packetEventsEnabled) {
                                    indicateChestMatch(getModule(), player, container);
                                } else {
                                    fallbackIndicateMatch(player, container.getLocation().add(0.5, 0.5, 0.5));
                                }
                            } else {
                                if (packetEventsEnabled) {
                                    indicateContainerMatch(getModule(), player, container);
                                } else {
                                    fallbackIndicateMatch(player, container.getLocation().add(0.5, 0.5, 0.5));
                                }
                            }
                            anyFound = true;
                        }
                    }
                }
                if (configSearchEntities) {
                    for (final var entity : chunk.getEntities()) {
                        // Don't indicate the player
                        if (entity == player) {
                            continue;
                        }

                        if (entity instanceof InventoryHolder holder) {
                            if (holder.getInventory().contains(material)) {
                                if (packetEventsEnabled) {
                                    indicateEntityMatch(getModule(), player, entity);
                                } else {
                                    fallbackIndicateMatch(player, entity.getLocation());
                                }
                                anyFound = true;
                            }
                        }
                    }
                }
            }
        }

        if (anyFound) {
            player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.MASTER, 1.0f, 1.3f);
        } else {
            player.playSound(player, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 5.0f);
        }

        return anyFound;
    }
}