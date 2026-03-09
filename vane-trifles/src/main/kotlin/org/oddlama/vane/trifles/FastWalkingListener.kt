package org.oddlama.vane.trifles

import io.papermc.paper.event.entity.EntityMoveEvent
import net.minecraft.world.entity.monster.Monster
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerMoveEvent
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.core.Listener

class FastWalkingListener(var fastWalking: FastWalkingGroup) : Listener<Trifles?>(fastWalking) {
    @ConfigBoolean(def = false, desc = "Whether hostile mobs should be allowed to fast walk on paths.")
    var configHostileSpeedwalk: Boolean = false

    @ConfigBoolean(def = true, desc = "Whether villagers should be allowed to fast walk on paths.")
    var configVillagerSpeedwalk: Boolean = false

    @ConfigBoolean(
        def = false,
        desc = "Whether players should be the only entities allowed to fast walk on paths (will override other path walk settings)."
    )
    var configPlayersOnlySpeedwalk: Boolean = false

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        // Players mustn't be flying
        val player = event.getPlayer()
        if (player.isGliding) {
            return
        }

        var effectEntity: LivingEntity = player
        if (player.isInsideVehicle && player.vehicle is LivingEntity) {
            effectEntity = player.vehicle as LivingEntity
        }

        // Inspect a block type just a little below
        val block = effectEntity.location.clone().subtract(0.0, 0.1, 0.0).block
        if (!fastWalking.configMaterials!!.contains(block.type)) {
            return
        }

        // Apply potion effect
        effectEntity.addPotionEffect(fastWalking.walkSpeedEffect!!)
    }

    // This is fired for entities except players.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityMove(event: EntityMoveEvent) {
        val entity = event.getEntity()

        // Cancel event if speedwalking is only enabled for players
        if (configPlayersOnlySpeedwalk) return

        // Cancel event if speedwalking is disabled for Hostile mobs
        if (entity is Monster && !configHostileSpeedwalk) return

        // Cancel event if speedwalking is disabled for villagers
        if (entity.type == EntityType.VILLAGER && !configVillagerSpeedwalk) return

        // Inspect a block type just a little below
        val block = event.to.clone().subtract(0.0, 0.1, 0.0).block
        if (!fastWalking.configMaterials!!.contains(block.type)) {
            return
        }

        // Apply potion effect
        entity.addPotionEffect(fastWalking.walkSpeedEffect!!)
    }
}
