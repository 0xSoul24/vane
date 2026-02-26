package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.PlayerUtil.giveItems;
import static org.oddlama.vane.util.PlayerUtil.swingArm;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Slime;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "slime_bucket", base = Material.SLIME_BALL, modelData = 0x760014 /* and 0x760015 */, version = 1)
public class SlimeBucket extends CustomItem<Trifles> {
    private static final float CUSTOM_MODEL_DATA_QUIET = 0x760014;
    private static final float CUSTOM_MODEL_DATA_JUMPY = 0x760015;
    private HashSet<UUID> playersInSlimeChunks = new HashSet<>();

    public SlimeBucket(Context<Trifles> context) {
        super(context);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        final var entity = event.getRightClicked();
        // Only when a tiny slime is right-clicked
        if (!(entity instanceof Slime slime) || slime.getSize() != 1) {
            return;
        }

        if (entity.isDead()) {
            return;
        }

        // With an empty bucket in the main hand
        final var player = event.getPlayer();
        final var itemInHand = player.getEquipment().getItem(event.getHand());
        if (itemInHand.getType() != Material.BUCKET) {
            return;
        }

        // Consume one bucket to create a slime bucket.
        entity.remove();
        swingArm(player, event.getHand());
        player.playSound(player, Sound.ENTITY_SLIME_JUMP, SoundCategory.MASTER, 1.0f, 2.0f);

        // Create slime bucket with correct custom model data
        final var newStack = newStack();
        newStack.editMeta(meta -> {
            final var correctModelData = player.getChunk().isSlimeChunk()
               ? CUSTOM_MODEL_DATA_JUMPY
               : CUSTOM_MODEL_DATA_QUIET;
            CustomModelDataComponent customModelDataComponent = meta.getCustomModelDataComponent();
            customModelDataComponent.setFloats(List.of(correctModelData));
            meta.setCustomModelDataComponent(customModelDataComponent);
        });

        if (itemInHand.getAmount() == 1) {
            // Replace with Slime Bucket
            player.getEquipment().setItem(event.getHand(), newStack);
        } else {
            // Reduce the amount and add SlimeBucket to inventory
            itemInHand.setAmount(itemInHand.getAmount() - 1);
            giveItems(player, newStack, 1);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        // Skip if no block was right-clicked
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // With a slimeBucket in main hand
        final var player = event.getPlayer();
        final var itemInHand = player.getEquipment().getItem(event.getHand());
        final var customItem = getModule().getCore().itemRegistry().get(itemInHand);
        if (!(customItem instanceof SlimeBucket slimeBucket) || !slimeBucket.enabled()) {
            return;
        }

        // Prevent offhand from triggering (e.g., placing torches)
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        // Place slime back into the world
        final var loc = event.getInteractionPoint();
        loc
            .getWorld()
            .spawnEntity(loc, EntityType.SLIME, CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                if (entity instanceof Slime slime) {
                    slime.setSize(1);
                }
            });

        player.playSound(player, Sound.ENTITY_SLIME_JUMP, SoundCategory.MASTER, 1.0f, 2.0f);
        swingArm(player, event.getHand());
        if (itemInHand.getAmount() == 1) {
            // Replace with empty bucket
            player.getEquipment().setItem(event.getHand(), new ItemStack(Material.BUCKET));
        } else {
            // Reduce the amount and add empty bucket to inventory
            itemInHand.setAmount(itemInHand.getAmount() - 1);
            giveItems(player, new ItemStack(Material.BUCKET), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        final var player = event.getPlayer();
        final var inSlimeChunk = event.getTo().getChunk().isSlimeChunk();
        final var inSet = playersInSlimeChunks.contains(player.getUniqueId());

        if (inSet != inSlimeChunk) {
            if (inSlimeChunk) {
                playersInSlimeChunks.add(player.getUniqueId());
            } else {
                playersInSlimeChunks.remove(player.getUniqueId());
            }

            final var correctModelData = inSlimeChunk ? CUSTOM_MODEL_DATA_JUMPY : CUSTOM_MODEL_DATA_QUIET;
            for (final var item : player.getInventory().getContents()) {
                final var customItem = getModule().getCore().itemRegistry().get(item);
                if (customItem instanceof SlimeBucket slimeBucket && slimeBucket.enabled()) {
                    // Update slime bucket custom model data
                    item.editMeta(meta -> {
                        CustomModelDataComponent customModelDataComponent = meta.getCustomModelDataComponent();
                        customModelDataComponent.setFloats(List.of(correctModelData));
                        meta.setCustomModelDataComponent(customModelDataComponent);
                    });
                }
            }
        }
    }
}
