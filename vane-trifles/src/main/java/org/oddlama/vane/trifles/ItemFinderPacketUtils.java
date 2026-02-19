package org.oddlama.vane.trifles;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class ItemFinderPacketUtils {
    private final static long GLOW_DURATION = 20L * 8L; // 8 Seconds
    private final static float CONTAINER_DEFAULT_SCALE = 0.95f;
    private final static float CONTAINER_DEFAULT_TRANSLATION = (1 - CONTAINER_DEFAULT_SCALE) * 0.5f;
    private final static float CHEST_DEFAULT_SCALE = 0.85f;

    static void indicateEntityMatch(@NotNull Trifles module, @NotNull Player player, @NotNull Entity entity) {
        // Get base entity data
        var baseEntityData = SpigotConversionUtil.getEntityMetadata(entity);

        // Generate metadata with glowing turned on
        var glowingEntityData = new ArrayList<>(baseEntityData);
        var glowData = new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x40);
        glowingEntityData.add(glowData);

        // Create packet and set glowing metadata
        var glowingPacket = new WrapperPlayServerEntityMetadata(
                entity.getEntityId(),
                glowingEntityData
        );

        // Send packet
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, glowingPacket);

        // Schedule repeating packet b/c if the player looks away from entities, they will stop glowing
        var repeatingPacketTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                }

                PacketEvents.getAPI().getPlayerManager().sendPacket(player, glowingPacket);
            }
        }.runTaskTimer(module, 1, 1);

        // Create non-glowing packet
        var nonGlowingEntityData = new ArrayList<>(baseEntityData);
        WrapperPlayServerEntityMetadata nonGlowingPacket = new WrapperPlayServerEntityMetadata(
                entity.getEntityId(),
                nonGlowingEntityData
        );

        // Run task later to send packet to stop glowing
        new ResetGlowingPacketTask(player, nonGlowingPacket, repeatingPacketTask).runTaskLater(module, GLOW_DURATION);
    }

    static void indicateContainerMatch(@NotNull Trifles module, @NotNull Player player, @NotNull Container container) {
        int displayId = SpigotReflectionUtil.generateEntityId();
        // Check for Shulkers so we can handle them differently
        var entityType = (container instanceof ShulkerBox) ? EntityTypes.SHULKER : EntityTypes.BLOCK_DISPLAY;
        var displayPacket = new WrapperPlayServerSpawnEntity(
                displayId,
                UUID.randomUUID(),
                entityType,
                SpigotConversionUtil.fromBukkitLocation(container.getLocation()),
                container.getLocation().getYaw(),
                0,
                null
        );

        // Metadata
        var displayMetadata = new ArrayList<EntityData<?>>();
        displayMetadata.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) (0x20 | 0x40)));
        if (entityType == EntityTypes.BLOCK_DISPLAY) {
            displayMetadata.add(new EntityData<>(11, EntityDataTypes.VECTOR3F, new Vector3f(CONTAINER_DEFAULT_TRANSLATION, CONTAINER_DEFAULT_TRANSLATION, CONTAINER_DEFAULT_TRANSLATION)));
            displayMetadata.add(new EntityData<>(12, EntityDataTypes.VECTOR3F, new Vector3f(CONTAINER_DEFAULT_SCALE, CONTAINER_DEFAULT_SCALE, CONTAINER_DEFAULT_SCALE)));
            displayMetadata.add(new EntityData<>(23, EntityDataTypes.BLOCK_STATE, SpigotConversionUtil.fromBukkitBlockData(container.getBlockData()).getGlobalId()));
        }
        var displayMetadataPacket = new WrapperPlayServerEntityMetadata(
                displayId,
                displayMetadata
        );

        // Send packets
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, displayPacket);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, displayMetadataPacket);

        // Shulker specific scale attribute packet
        if (entityType == EntityTypes.SHULKER) {
            var scaleAttribute = Attributes.SCALE;

            // Create scale attribute modifier
            var attributeModifier = new WrapperPlayServerUpdateAttributes.PropertyModifier(
                    UUID.randomUUID(),
                    CONTAINER_DEFAULT_SCALE - 1,
                    WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.ADDITION
            );
            // Create scale attribute base property
            var attributeProperty = new WrapperPlayServerUpdateAttributes.Property(
                    scaleAttribute,
                    1.0,
                    Collections.singletonList(attributeModifier)
            );
            // Create update attribute packet
            var attributePacket = new WrapperPlayServerUpdateAttributes(
                    displayId,
                    Collections.singletonList(attributeProperty)
            );

            // Send packet
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, attributePacket);
        }

        // Construct destroy entity packet
        var destroyEntityPacket = new WrapperPlayServerDestroyEntities(displayId);

        // Run task later to remove entity
        new ResetGlowingPacketTask(player, destroyEntityPacket, null).runTaskLater(module, GLOW_DURATION);
    }

    // Special function for chests
    static void indicateChestMatch(@NotNull Trifles module, @NotNull Player player, @NotNull Container container) {
        int displayId = SpigotReflectionUtil.generateEntityId();
        var displayPacket = new WrapperPlayServerSpawnEntity(
                displayId,
                UUID.randomUUID(),
                EntityTypes.SHULKER,
                SpigotConversionUtil.fromBukkitLocation(container.getLocation()),
                container.getLocation().getYaw(),
                0,
                null
        );

        // Metadata
        var displayMetadata = new ArrayList<EntityData<?>>();
        displayMetadata.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) (0x20 | 0x40)));
        var displayMetadataPacket = new WrapperPlayServerEntityMetadata(
                displayId,
                displayMetadata
        );

        // Send packets
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, displayPacket);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, displayMetadataPacket);

        // Shulker specific scale attribute packet
        var scaleAttribute = Attributes.SCALE;

        // Create scale attribute modifier
        var attributeModifier = new WrapperPlayServerUpdateAttributes.PropertyModifier(
                UUID.randomUUID(),
                CHEST_DEFAULT_SCALE - 1,
                WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.ADDITION
        );
        // Create scale attribute base property
        var attributeProperty = new WrapperPlayServerUpdateAttributes.Property(
                scaleAttribute,
                1.0,
                Collections.singletonList(attributeModifier)
        );
        // Create update attribute packet
        var attributePacket = new WrapperPlayServerUpdateAttributes(
                displayId,
                Collections.singletonList(attributeProperty)
        );

        // Send packet
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, attributePacket);

        // Construct destroy entity packet
        var destroyEntityPacket = new WrapperPlayServerDestroyEntities(displayId);

        // Run task later to remove entity
        new ResetGlowingPacketTask(player, destroyEntityPacket, null).runTaskLater(module, GLOW_DURATION);
    }

    private static class ResetGlowingPacketTask extends BukkitRunnable {
        private final Player player;
        private final PacketWrapper<?> packet;
        private final BukkitTask optionalRepeatingPacketTask;

        public ResetGlowingPacketTask(@NotNull Player player, @NotNull PacketWrapper<?> packet, @Nullable BukkitTask optionalRepeatingPacketTask) {
            this.player = player;
            this.packet = packet;
            this.optionalRepeatingPacketTask = optionalRepeatingPacketTask;
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                this.cancel();
            }

            if (optionalRepeatingPacketTask != null && !optionalRepeatingPacketTask.isCancelled()) {
                optionalRepeatingPacketTask.cancel();
            }

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }
    }
}
