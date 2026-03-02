package org.oddlama.vane.admin

import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityBreakDoorEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigStringList
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context

class HazardProtection(context: Context<Admin?>) : Listener<Admin?>(
    context.group(
        "HazardProtection",
        "Enable hazard protection. The options below allow more fine-grained control over the hazards to protect from."
    )
) {
    private val worldRebuild: WorldRebuild = WorldRebuild(getContext()!!)

    @ConfigBoolean(def = true, desc = "Restrict wither spawning to a list of worlds defined by wither_world_whitelist.")
    private val configEnableWitherWorldWhitelist = false

    @ConfigStringList(
        def = ["world_nether", "world_the_end"],
        desc = "A list of worlds in which the wither may be spawned."
    )
    private val configWitherWorldWhitelist: MutableList<String?>? = null

    @ConfigBoolean(def = true, desc = "Disables explosions from the wither.")
    private val configDisableWitherExplosions = false

    @ConfigBoolean(def = true, desc = "Disables explosions from creepers.")
    private val configDisableCreeperExplosions = false

    @ConfigBoolean(def = true, desc = "Disables enderman block pickup.")
    private val configDisableEndermanBlockPickup = false

    @ConfigBoolean(def = true, desc = "Disables entities from breaking doors (various zombies).")
    private val configDisableDoorBreaking = false

    @ConfigBoolean(def = true, desc = "Disables fire from lightning.")
    private val configDisableLightningFire = false

    @LangMessage
    private val langWitherSpawnProhibited: TranslatedMessage? = null

    private fun disableExplosion(type: EntityType): Boolean {
        return when (type) {
            EntityType.WITHER, EntityType.WITHER_SKULL -> configDisableWitherExplosions
            EntityType.CREEPER -> configDisableCreeperExplosions
            else -> false
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (disableExplosion(event.entityType)) {
            if (worldRebuild.enabled()) {
                // Schedule rebuild
                worldRebuild.rebuild(event.blockList())
                // Remove all affected blocks from event
                event.blockList().clear()
            } else {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent) {
        if (disableExplosion(event.remover.type)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityBreakDoor(event: EntityBreakDoorEvent) {
        if (configDisableDoorBreaking) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockIgnite(event: BlockIgniteEvent) {
        if (event.cause == BlockIgniteEvent.IgniteCause.LIGHTNING && configDisableLightningFire) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (!configEnableWitherWorldWhitelist) {
            return
        }

        // Only for wither spawns
        if (event.getEntity().type != EntityType.WITHER) {
            return
        }

        // Check if the world is whitelisted
        val world = event.getEntity().world
        if (configWitherWorldWhitelist!!.contains(world.name)) {
            return
        }

        langWitherSpawnProhibited!!.broadcastWorld(world, world.name)
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityBlockChange(event: EntityChangeBlockEvent) {
        if (!configDisableEndermanBlockPickup) {
            return
        }

        // Only for enderman events
        if (event.getEntity().type != EntityType.ENDERMAN) {
            return
        }

        event.isCancelled = true
    }
}
