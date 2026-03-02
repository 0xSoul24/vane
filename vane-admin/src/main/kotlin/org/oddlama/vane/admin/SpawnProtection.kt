package org.oddlama.vane.admin

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.player.*
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import kotlin.math.sqrt

class SpawnProtection(context: Context<Admin?>) : Listener<Admin?>(
    context.groupDefaultDisabled(
        "SpawnProtection",
        "Enable spawn protection. Slightly more sophisticated than the vanilla spawn protection, if you need even more control, use regions. This will prevent anyone from modifying the spawn of the world if they don't have the permission '" +
                PERMISSION_NAME +
                "'."
    )
) {
    private val permission = Permission(
        PERMISSION_NAME,
        "Allow player to bypass spawn protection",
        PermissionDefault.OP
    )

    @ConfigBoolean(def = true, desc = "Allow interaction events at spawn (buttons, levers, etc.).")
    private val configAllowInteraction = false

    @ConfigInt(def = 64, min = 0, desc = "Radius to protect.")
    private val configRadius = 0

    @ConfigString(def = "world", desc = "The spawn world.")
    private val configWorld: String? = null

    @ConfigBoolean(def = true, desc = "Use world's spawn location instead of the specified center coordinates.")
    private val configUseSpawnLocation = false

    @ConfigInt(def = 0, desc = "Center X coordinate.")
    private val configX = 0

    @ConfigInt(def = 0, desc = "Center Z coordinate.")
    private val configZ = 0

    private var spawnCenter: Location? = null

    init {
        module!!.registerPermission(permission)
    }

    public override fun onConfigChange() {
        spawnCenter = null
        scheduleNextTick {
            val world: World? = module!!.server.getWorld(configWorld!!)
            if (world == null) {
                // todo print error and show valid worlds.
                module!!.log.warning(
                    "The world \"$configWorld\" configured for spawn-protection could not be found."
                )
                module!!.log.warning("These are the names of worlds existing on this server:")
                for (w in module!!.server.worlds) {
                    module!!.log.warning("  \"" + w.name + "\"")
                }
                spawnCenter = null
            } else {
                if (configUseSpawnLocation) {
                    spawnCenter = world.spawnLocation
                    spawnCenter!!.y = 0.0
                } else {
                    spawnCenter = Location(world, configX.toDouble(), 0.0, configZ.toDouble())
                }
            }
        }
    }

    fun denyModifySpawn(block: Block, entity: Entity?): Boolean {
        return denyModifySpawn(block.location, entity)
    }

    fun denyModifySpawn(location: Location, entity: Entity?): Boolean {
        if (spawnCenter == null || entity !is Player) {
            return false
        }

        val dx = location.x - spawnCenter!!.x
        val dz = location.z - spawnCenter!!.z
        val distance = sqrt(dx * dx + dz * dz)
        if (distance > configRadius) {
            return false
        }

        return !entity.hasPermission(permission)
    }

    /* ************************ blocks ************************ */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (denyModifySpawn(event.getBlock(), event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (denyModifySpawn(event.getBlock(), event.getPlayer())) {
            event.isCancelled = true
        }
    }

    /* ************************ hanging ************************ */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent) {
        if (denyModifySpawn(event.entity.location, event.remover)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onHangingPlace(event: HangingPlaceEvent) {
        if (denyModifySpawn(event.entity.location, event.player)) {
            event.isCancelled = true
        }
    }

    /* ************************ player ************************ */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        if (denyModifySpawn(event.rightClicked.location, event.getPlayer())) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (denyModifySpawn(event.block, event.getPlayer())) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerBucketFill(event: PlayerBucketFillEvent) {
        if (denyModifySpawn(event.block, event.getPlayer())) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (!configAllowInteraction && denyModifySpawn(event.rightClicked.location, event.getPlayer())) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.clickedBlock != null && !configAllowInteraction &&
            denyModifySpawn(event.clickedBlock!!, event.getPlayer())
        ) {
            event.setCancelled(true)
        }
    }

    companion object {
        private const val PERMISSION_NAME = "vane.admin.bypass_spawn_protection"
    }
}
