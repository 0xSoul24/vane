package org.oddlama.vane.trifles

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.attribute.Attributes
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.PropertyModifier
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import org.bukkit.block.Container
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*

object ItemFinderPacketUtils {
    private const val GLOW_DURATION = 20L * 8L // 8 Seconds
    private const val CONTAINER_DEFAULT_SCALE = 0.95f
    private const val CONTAINER_DEFAULT_TRANSLATION = (1 - CONTAINER_DEFAULT_SCALE) * 0.5f
    private const val CHEST_DEFAULT_SCALE = 0.85f

    fun indicateEntityMatch(module: Trifles, player: Player, entity: Entity) {
        // Get base entity data
        val baseEntityData = SpigotConversionUtil.getEntityMetadata(entity)

        // Generate metadata with glowing turned on
        val glowingEntityData = ArrayList(baseEntityData)
        val glowData = EntityData(0, EntityDataTypes.BYTE, 0x40.toByte())
        glowingEntityData.add(glowData)

        // Create packet and set glowing metadata
        val glowingPacket = WrapperPlayServerEntityMetadata(
            entity.entityId,
            glowingEntityData
        )

        // Send packet
        PacketEvents.getAPI().playerManager.sendPacket(player, glowingPacket)

        // Schedule repeating packet b/c if the player looks away from entities, they will stop glowing
        val repeatingPacketTask = object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline) {
                    this.cancel()
                }

                PacketEvents.getAPI().playerManager.sendPacket(player, glowingPacket)
            }
        }.runTaskTimer(module, 1, 1)

        // Create non-glowing packet
        val nonGlowingEntityData = ArrayList(baseEntityData)
        val nonGlowingPacket = WrapperPlayServerEntityMetadata(
            entity.entityId,
            nonGlowingEntityData
        )

        // Run task later to send packet to stop glowing
        ResetGlowingPacketTask(player, nonGlowingPacket, repeatingPacketTask).runTaskLater(module, GLOW_DURATION)
    }

    fun indicateContainerMatch(module: Trifles, player: Player, container: Container) {
        val displayId = SpigotReflectionUtil.generateEntityId()
        // Check for Shulkers so we can handle them differently
        val entityType = if (container is ShulkerBox) EntityTypes.SHULKER else EntityTypes.BLOCK_DISPLAY
        val displayPacket = WrapperPlayServerSpawnEntity(
            displayId,
            UUID.randomUUID(),
            entityType,
            SpigotConversionUtil.fromBukkitLocation(container.location),
            container.location.yaw,
            0,
            null
        )

        // Metadata
        val displayMetadata = ArrayList<EntityData<*>?>()
        displayMetadata.add(EntityData(0, EntityDataTypes.BYTE, (0x20 or 0x40).toByte()))
        if (entityType === EntityTypes.BLOCK_DISPLAY) {
            displayMetadata.add(
                EntityData(
                    11, EntityDataTypes.VECTOR3F, Vector3f(
                        CONTAINER_DEFAULT_TRANSLATION, CONTAINER_DEFAULT_TRANSLATION, CONTAINER_DEFAULT_TRANSLATION
                    )
                )
            )
            displayMetadata.add(
                EntityData(
                    12,
                    EntityDataTypes.VECTOR3F,
                    Vector3f(CONTAINER_DEFAULT_SCALE, CONTAINER_DEFAULT_SCALE, CONTAINER_DEFAULT_SCALE)
                )
            )
            displayMetadata.add(
                EntityData(
                    23,
                    EntityDataTypes.BLOCK_STATE,
                    SpigotConversionUtil.fromBukkitBlockData(container.blockData).globalId
                )
            )
        }
        val displayMetadataPacket = WrapperPlayServerEntityMetadata(
            displayId,
            displayMetadata
        )

        // Send packets
        PacketEvents.getAPI().playerManager.sendPacket(player, displayPacket)
        PacketEvents.getAPI().playerManager.sendPacket(player, displayMetadataPacket)

        // Shulker specific scale attribute packet
        if (entityType === EntityTypes.SHULKER) {
            val scaleAttribute = Attributes.SCALE

            // Create scale attribute modifier
            val attributeModifier = PropertyModifier(
                UUID.randomUUID(),
                (CONTAINER_DEFAULT_SCALE - 1).toDouble(),
                PropertyModifier.Operation.ADDITION
            )
            // Create scale attribute base property
            val attributeProperty = WrapperPlayServerUpdateAttributes.Property(
                scaleAttribute,
                1.0,
                mutableListOf<PropertyModifier?>(attributeModifier)
            )
            // Create update attribute packet
            val attributePacket = WrapperPlayServerUpdateAttributes(
                displayId,
                mutableListOf<WrapperPlayServerUpdateAttributes.Property?>(attributeProperty)
            )

            // Send packet
            PacketEvents.getAPI().playerManager.sendPacket(player, attributePacket)
        }

        // Construct destroy entity packet
        val destroyEntityPacket = WrapperPlayServerDestroyEntities(displayId)

        // Run task later to remove entity
        ResetGlowingPacketTask(player, destroyEntityPacket, null).runTaskLater(module, GLOW_DURATION)
    }

    // Special function for chests
    fun indicateChestMatch(module: Trifles, player: Player, container: Container) {
        val displayId = SpigotReflectionUtil.generateEntityId()
        val displayPacket = WrapperPlayServerSpawnEntity(
            displayId,
            UUID.randomUUID(),
            EntityTypes.SHULKER,
            SpigotConversionUtil.fromBukkitLocation(container.location),
            container.location.yaw,
            0,
            null
        )

        // Metadata
        val displayMetadata = ArrayList<EntityData<*>?>()
        displayMetadata.add(EntityData(0, EntityDataTypes.BYTE, (0x20 or 0x40).toByte()))
        val displayMetadataPacket = WrapperPlayServerEntityMetadata(
            displayId,
            displayMetadata
        )

        // Send packets
        PacketEvents.getAPI().playerManager.sendPacket(player, displayPacket)
        PacketEvents.getAPI().playerManager.sendPacket(player, displayMetadataPacket)

        // Shulker specific scale attribute packet
        val scaleAttribute = Attributes.SCALE

        // Create scale attribute modifier
        val attributeModifier = PropertyModifier(
            UUID.randomUUID(),
            (CHEST_DEFAULT_SCALE - 1).toDouble(),
            PropertyModifier.Operation.ADDITION
        )
        // Create scale attribute base property
        val attributeProperty = WrapperPlayServerUpdateAttributes.Property(
            scaleAttribute,
            1.0,
            mutableListOf<PropertyModifier?>(attributeModifier)
        )
        // Create update attribute packet
        val attributePacket = WrapperPlayServerUpdateAttributes(
            displayId,
            mutableListOf<WrapperPlayServerUpdateAttributes.Property?>(attributeProperty)
        )

        // Send packet
        PacketEvents.getAPI().playerManager.sendPacket(player, attributePacket)

        // Construct destroy entity packet
        val destroyEntityPacket = WrapperPlayServerDestroyEntities(displayId)

        // Run task later to remove entity
        ResetGlowingPacketTask(player, destroyEntityPacket, null).runTaskLater(module, GLOW_DURATION)
    }

    private class ResetGlowingPacketTask(
        private val player: Player,
        private val packet: PacketWrapper<*>,
        private val optionalRepeatingPacketTask: BukkitTask?
    ) : BukkitRunnable() {
        override fun run() {
            if (!player.isOnline) {
                this.cancel()
            }

            if (optionalRepeatingPacketTask != null && !optionalRepeatingPacketTask.isCancelled) {
                optionalRepeatingPacketTask.cancel()
            }

            PacketEvents.getAPI().playerManager.sendPacket(player, packet)
        }
    }
}
