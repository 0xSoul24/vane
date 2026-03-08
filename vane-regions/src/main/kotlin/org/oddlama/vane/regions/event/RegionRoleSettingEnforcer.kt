package org.oddlama.vane.regions.event

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.block.DoubleChest
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryInteractEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.*
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.regions.region.Region
import org.oddlama.vane.regions.region.RoleSetting

class RegionRoleSettingEnforcer(context: Context<Regions?>?) : Listener<Regions?>(context) {
    fun checkSettingAt(
        location: Location,
        player: Player,
        setting: RoleSetting,
        checkAgainst: Boolean
    ): Boolean {
        val region: Region = module!!.regionAt(location) ?: return false

        val group = region.regionGroup(module!!)
        return group!!.getRole(player.uniqueId)!!.getSetting(setting) == checkAgainst
    }

    fun checkSettingAt(
        block: Block,
        player: Player,
        setting: RoleSetting,
        checkAgainst: Boolean
    ): Boolean {
        val region: Region = module!!.regionAt(block) ?: return false

        val group = region.regionGroup(module!!)
        return group!!.getRole(player.uniqueId)!!.getSetting(setting) == checkAgainst
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        // Prevent breaking of region blocks
        if (checkSettingAt(event.getBlock(), event.player, RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        // Prevent (re-)placing of region blocks
        if (checkSettingAt(event.getBlock(), event.getPlayer(), RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damaged = event.getEntity()
        val damager = event.damager

        when (damaged.type) {
            EntityType.ARMOR_STAND -> {
                if (damager !is Player) {
                    return
                }

                if (checkSettingAt(damaged.location.block, damager, RoleSetting.BUILD, false)) {
                    event.isCancelled = true
                }
                return
            }

            EntityType.ITEM_FRAME -> {
                if (damager !is Player) {
                    return
                }

                val itemFrame = damaged as ItemFrame
                val item = itemFrame.item
                if (item.type != Material.AIR) {
                    // This is a player taking the item out of an item-frame
                    if (checkSettingAt(damaged.location.block, damager, RoleSetting.CONTAINER, false)
                    ) {
                        event.isCancelled = true
                    }
                }
            }

            else -> return
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent) {
        val remover = event.remover
        var player: Player? = null

        if (remover is Player) {
            player = remover
        } else if (remover is Projectile) {
            val shooter = remover.shooter
            if (shooter is Player) {
                player = shooter
            }
        }

        if (player != null && checkSettingAt(event.entity.location, player, RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingPlace(event: HangingPlaceEvent) {
        if (checkSettingAt(event.entity.location, event.player!!, RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        if (checkSettingAt(event.rightClicked.location, event.getPlayer(), RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (checkSettingAt(event.blockClicked, event.getPlayer(), RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerBucketFill(event: PlayerBucketFillEvent) {
        if (checkSettingAt(event.blockClicked, event.getPlayer(), RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        if (entity.type != EntityType.ITEM_FRAME) {
            return
        }

        // Place or rotate item
        if (checkSettingAt(entity.location, event.getPlayer(), RoleSetting.CONTAINER, false)) {
            event.isCancelled = true
        }
    }

    // The EventPriority is HIGH, so this is executed AFTER the portals try
    // to activate, which is a seperate permission.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.getPlayer()
        val block = event.clickedBlock ?: return

        when (event.action) {
            Action.PHYSICAL -> {
                if (Tag.PRESSURE_PLATES.isTagged(block.type)) {
                    if (checkSettingAt(block, player, RoleSetting.USE, false)) {
                        event.isCancelled = true
                    }
                } else if (block.type == Material.TRIPWIRE) {
                    if (checkSettingAt(block, player, RoleSetting.USE, false)) {
                        event.isCancelled = true
                    }
                }
                return
            }

            Action.RIGHT_CLICK_BLOCK -> {
                if (checkSettingAt(block, player, RoleSetting.USE, false)) {
                    event.isCancelled = true
                }
            }

            else -> return
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInventoryOpen(event: InventoryOpenEvent) {
        // Only relevant if viewing should be prohibited, too.
        if (!module!!.configProhibitViewingContainers) {
            return
        }

        if (event.player !is Player) {
            return
        }

        val player = event.player as Player
        if (checkContainerSettingAt(event.inventory, player)) {
            event.isCancelled = true
        }
    }

    private fun checkContainerSettingAt(inventory: org.bukkit.inventory.Inventory, player: Player): Boolean {
        if (inventory.location == null || inventory.holder == null) {
            // Inventory is virtual / transient
            return false
        }

        val holder = inventory.holder
        return (holder is DoubleChest || holder is Container || holder is Minecart)
            && checkSettingAt(inventory.location!!, player, RoleSetting.CONTAINER, false)
    }

    fun onPlayerInventoryInteract(event: InventoryInteractEvent) {
        val clicker = event.whoClicked
        if (clicker !is Player) {
            return
        }

        if (checkContainerSettingAt(event.inventory, clicker)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInventoryClick(event: InventoryClickEvent) {
        onPlayerInventoryInteract(event)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInventoryDrag(event: InventoryDragEvent) {
        onPlayerInventoryInteract(event)
    }
}
