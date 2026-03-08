package org.oddlama.vane.regions.event

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.*
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.regions.region.EnvironmentSetting
import org.oddlama.vane.regions.region.Region

class RegionEnvironmentSettingEnforcer(context: Context<Regions?>?) : Listener<Regions?>(context) {
    fun checkSettingAt(
        location: Location,
        setting: EnvironmentSetting,
        checkAgainst: Boolean
    ): Boolean {
        val region: Region = module!!.regionAt(location) ?: return false

        val group = region.regionGroup(module!!)
        return group!!.getSetting(setting) == checkAgainst
    }

    fun checkSettingAt(block: Block, setting: EnvironmentSetting, checkAgainst: Boolean): Boolean {
        val region: Region = module!!.regionAt(block) ?: return false

        val group = region.regionGroup(module!!)
        return group!!.getSetting(setting) == checkAgainst
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        // Prevent explosions from removing region blocks
        event.blockList().removeIf { block: Block? -> checkSettingAt(block!!, EnvironmentSetting.EXPLOSIONS, false) }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        // Prevent explosions from removing region blocks
        event.blockList().removeIf { block: Block? -> checkSettingAt(block!!, EnvironmentSetting.EXPLOSIONS, false) }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (event.getEntity() !is Monster) {
            return
        }

        // Prevent monster entities from changing region blocks
        if (checkSettingAt(event.block, EnvironmentSetting.MONSTERS, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        if (checkSettingAt(event.getBlock(), EnvironmentSetting.FIRE, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockSpread(event: BlockSpreadEvent) {
        val setting = when (event.newState.type) {
            Material.FIRE -> EnvironmentSetting.FIRE
            Material.VINE -> EnvironmentSetting.VINE_GROWTH
            else -> return
        }

        if (checkSettingAt(event.getBlock(), setting, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        // Only cancel natural spawns and alike
        when (event.spawnReason) {
            CreatureSpawnEvent.SpawnReason.JOCKEY, CreatureSpawnEvent.SpawnReason.MOUNT, CreatureSpawnEvent.SpawnReason.NATURAL -> {}
            else -> return
        }

        val entity = event.getEntity()
        if (entity is Monster) {
            if (checkSettingAt(event.location, EnvironmentSetting.MONSTERS, false)) {
                event.isCancelled = true
            }
        } else if (entity is Animals) {
            if (checkSettingAt(event.location, EnvironmentSetting.ANIMALS, false)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damaged = event.getEntity()
        val damager = event.damager

        if (damaged.type != EntityType.PLAYER) {
            return
        }

        val playerDamaged = damaged as Player
        val playerDamager = when (damager) {
            is Player -> {
                damager
            }

            is Projectile if damager.shooter is Player -> {
                damager.shooter as Player?
            }

            else -> {
                return
            }
        }

        if (playerDamager != null && playerDamaged !== playerDamager &&
            (checkSettingAt(playerDamaged.location, EnvironmentSetting.PVP, false) ||
                    checkSettingAt(playerDamager.location, EnvironmentSetting.PVP, false))
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingBreakEvent(event: HangingBreakEvent) {
        when (event.cause) {
            RemoveCause.ENTITY -> return  // Handeled by onHangingBreakByEntity
            RemoveCause.EXPLOSION -> {
                if (checkSettingAt(event.entity.location, EnvironmentSetting.EXPLOSIONS, false)) {
                    event.isCancelled = true
                }
            }

            else -> return
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.PHYSICAL) {
            return
        }

        val block = event.clickedBlock
        if (block != null && block.type == Material.FARMLAND) {
            if (checkSettingAt(block, EnvironmentSetting.TRAMPLE, false)) {
                event.setCancelled(true)
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPotionSplash(event: PotionSplashEvent) {
        // Only if a player threw the potion check for PVP
        if (event.getEntity().shooter !is Player) {
            return
        }

        val thrower = event.getEntity().shooter as Player?
        val sourcePvpRestricted = checkSettingAt(thrower!!.location, EnvironmentSetting.PVP, false)

        // Cancel all damage to players if either thrower or damaged is
        // inside no-PVP region
        for (target in event.getAffectedEntities()) {
            if (target !is Player) {
                continue
            }

            if (sourcePvpRestricted || checkSettingAt(target.location, EnvironmentSetting.PVP, false)) {
                event.setIntensity(target, 0.0)
                return
            }
        }
    }
}
