package org.oddlama.vane.trifles.items

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Slime
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.PlayerUtil
import java.util.*
import java.util.function.Consumer

@VaneItem(name = "slime_bucket", base = Material.SLIME_BALL, modelData = 0x760014, version = 1)
class SlimeBucket(context: Context<Trifles?>) : org.oddlama.vane.core.item.CustomItem<Trifles?>(context) {
    private val playersInSlimeChunks = HashSet<UUID>()

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        // Only when a tiny slime is right-clicked
        if (entity !is Slime || entity.size != 1) {
            return
        }

        if (entity.isDead) {
            return
        }

        // With an empty bucket in the main hand
        val player = event.getPlayer()
        val itemInHand = player.equipment.getItem(event.hand)
        if (itemInHand.type != Material.BUCKET) {
            return
        }

        // Consume one bucket to create a slime bucket.
        entity.remove()
        PlayerUtil.swingArm(player, event.hand)
        player.playSound(player, Sound.ENTITY_SLIME_JUMP, SoundCategory.MASTER, 1.0f, 2.0f)

        // Create slime bucket with correct custom model data
        val newStack = newStack()
        newStack!!.editMeta(Consumer { meta: ItemMeta? ->
            val correctModelData: Float = if (player.chunk.isSlimeChunk)
                CUSTOM_MODEL_DATA_JUMPY
            else
                CUSTOM_MODEL_DATA_QUIET
            val customModelDataComponent = meta!!.customModelDataComponent
            customModelDataComponent.floats = listOf(correctModelData)
            meta.setCustomModelDataComponent(customModelDataComponent)
        })

        if (itemInHand.amount == 1) {
            // Replace with Slime Bucket
            player.equipment.setItem(event.hand, newStack)
        } else {
            // Reduce the amount and add SlimeBucket to inventory
            itemInHand.amount -= 1
            PlayerUtil.giveItems(player, newStack, 1)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Skip if no block was right-clicked
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        // With a slimeBucket in main hand
        val player = event.getPlayer()
        val itemInHand = player.equipment.getItem(event.hand!!)
        val customItem: CustomItem? = module!!.core?.itemRegistry()?.get(itemInHand)
        if (customItem !is SlimeBucket || !customItem.enabled()) {
            return
        }

        // Prevent offhand from triggering (e.g., placing torches)
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)

        // Place slime back into the world
        val loc = event.getInteractionPoint()
        loc!!
            .getWorld()
            .spawnEntity(loc, EntityType.SLIME, CreatureSpawnEvent.SpawnReason.CUSTOM, Consumer { entity: Entity? ->
                if (entity is Slime) {
                    entity.size = 1
                }
            })

        player.playSound(player, Sound.ENTITY_SLIME_JUMP, SoundCategory.MASTER, 1.0f, 2.0f)
        PlayerUtil.swingArm(player, event.hand!!)
        if (itemInHand.amount == 1) {
            // Replace with empty bucket
            player.equipment.setItem(event.hand!!, ItemStack(Material.BUCKET))
        } else {
            // Reduce the amount and add empty bucket to inventory
            itemInHand.amount -= 1
            PlayerUtil.giveItems(player, ItemStack(Material.BUCKET), 1)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.getPlayer()
        val inSlimeChunk = event.to.chunk.isSlimeChunk
        val inSet = playersInSlimeChunks.contains(player.uniqueId)

        if (inSet != inSlimeChunk) {
            if (inSlimeChunk) {
                playersInSlimeChunks.add(player.uniqueId)
            } else {
                playersInSlimeChunks.remove(player.uniqueId)
            }

            val correctModelData: Float = if (inSlimeChunk) CUSTOM_MODEL_DATA_JUMPY else CUSTOM_MODEL_DATA_QUIET
            for (item in player.inventory.contents) {
                val customItem: CustomItem? = module!!.core?.itemRegistry()?.get(item)
                if (customItem is SlimeBucket && customItem.enabled()) {
                    // Update slime bucket custom model data
                    item!!.editMeta(Consumer { meta: ItemMeta? ->
                        val customModelDataComponent = meta!!.customModelDataComponent
                        customModelDataComponent.floats = listOf(correctModelData)
                        meta.setCustomModelDataComponent(customModelDataComponent)
                    })
                }
            }
        }
    }

    companion object {
        private const val CUSTOM_MODEL_DATA_QUIET: Float = 0x760014.toFloat()
        private const val CUSTOM_MODEL_DATA_JUMPY: Float = 0x760015.toFloat()
    }
}
