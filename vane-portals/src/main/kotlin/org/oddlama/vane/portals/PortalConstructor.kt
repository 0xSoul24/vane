package org.oddlama.vane.portals

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigMaterial
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.functional.Function2
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.portals.event.PortalConstructEvent
import org.oddlama.vane.portals.event.PortalLinkConsoleEvent
import org.oddlama.vane.portals.portal.*
import org.oddlama.vane.portals.portal.PortalBoundary.Companion.searchAt
import org.oddlama.vane.portals.portal.PortalBoundary.ErrorState
import org.oddlama.vane.util.PlayerUtil
import java.util.*
import java.util.stream.Collectors
import kotlin.math.abs

class PortalConstructor(context: Context<Portals?>?) : Listener<Portals?>(context) {
    @JvmField
    @ConfigMaterial(def = Material.ENCHANTING_TABLE, desc = "The block used to build portal consoles.")
    var configMaterialConsole: Material? = null

    @JvmField
    @ConfigMaterial(def = Material.OBSIDIAN, desc = "The block used to build the portal boundary. Variation 1.")
    var configMaterialBoundary1: Material? = null

    @JvmField
    @ConfigMaterial(def = Material.CRYING_OBSIDIAN, desc = "The block used to build the portal boundary. Variation 2.")
    var configMaterialBoundary2: Material? = null

    @JvmField
    @ConfigMaterial(def = Material.GOLD_BLOCK, desc = "The block used to build the portal boundary. Variation 3.")
    var configMaterialBoundary3: Material? = null

    @JvmField
    @ConfigMaterial(
        def = Material.GILDED_BLACKSTONE,
        desc = "The block used to build the portal boundary. Variation 4."
    )
    var configMaterialBoundary4: Material? = null

    @JvmField
    @ConfigMaterial(def = Material.EMERALD_BLOCK, desc = "The block used to build the portal boundary. Variation 5.")
    var configMaterialBoundary5: Material? = null

    @JvmField
    @ConfigMaterial(def = Material.NETHERITE_BLOCK, desc = "The block used to build the portal origin.")
    var configMaterialOrigin: Material? = null

    @JvmField
    @ConfigMaterial(def = Material.AIR, desc = "The block used to build the portal area.")
    var configMaterialPortalArea: Material? = null

    @ConfigInt(def = 12, min = 1, desc = "Maximum horizontal distance between a console block and the portal.")
    var configConsoleMaxDistanceXz: Int = 0

    @ConfigInt(def = 12, min = 1, desc = "Maximum vertical distance between a console block and the portal.")
    var configConsoleMaxDistanceY: Int = 0

    @ConfigInt(
        def = 1024,
        min = 256,
        desc = "Maximum steps for the floodfill algorithm. This should only be increased if you want really big portals. It's recommended to keep this as low as possible."
    )
    var configAreaFloodfillMaxSteps: Int = 1024

    @ConfigInt(def = 24, min = 8, desc = "Maximum portal area width (bounding box will be measured).")
    var configAreaMaxWidth: Int = 0

    @ConfigInt(def = 24, min = 8, desc = "Maximum portal area height (bounding box will be measured).")
    var configAreaMaxHeight: Int = 24

    @ConfigInt(def = 128, min = 8, desc = "Maximum total amount of portal area blocks.")
    var configAreaMaxBlocks: Int = 128

    @LangMessage
    var langSelectBoundaryNow: TranslatedMessage? = null

    @LangMessage
    var langConsoleInvalidType: TranslatedMessage? = null

    @LangMessage
    var langConsoleDifferentWorld: TranslatedMessage? = null

    @LangMessage
    var langConsoleTooFarAway: TranslatedMessage? = null

    @LangMessage
    var langConsoleLinked: TranslatedMessage? = null

    @LangMessage
    var langNoBoundaryFound: TranslatedMessage? = null

    @LangMessage
    var langNoOrigin: TranslatedMessage? = null

    @LangMessage
    var langMultipleOrigins: TranslatedMessage? = null

    @LangMessage
    var langNoPortalBlockAboveOrigin: TranslatedMessage? = null

    @LangMessage
    var langNotEnoughPortalBlocksAboveOrigin: TranslatedMessage? = null

    @LangMessage
    var langTooLarge: TranslatedMessage? = null

    @LangMessage
    var langTooSmallSpawn: TranslatedMessage? = null

    @LangMessage
    var langTooManyPortalAreaBlocks: TranslatedMessage? = null

    @LangMessage
    var langPortalAreaObstructed: TranslatedMessage? = null

    @LangMessage
    var langIntersectsExistingPortal: TranslatedMessage? = null

    @LangMessage
    var langBuildRestricted: TranslatedMessage? = null

    @LangMessage
    var langLinkRestricted: TranslatedMessage? = null

    @LangMessage
    var langTargetAlreadyConnected: TranslatedMessage? = null

    @LangMessage
    var langSourceUseRestricted: TranslatedMessage? = null

    @LangMessage
    var langTargetUseRestricted: TranslatedMessage? = null

    private val portalBoundaryBuildMaterials: MutableSet<Material?> = HashSet<Material?>()

    private val pendingConsole = HashMap<UUID?, Block?>()

    public override fun onConfigChange() {
        portalBoundaryBuildMaterials.clear()
        portalBoundaryBuildMaterials.add(configMaterialBoundary1)
        portalBoundaryBuildMaterials.add(configMaterialBoundary2)
        portalBoundaryBuildMaterials.add(configMaterialBoundary3)
        portalBoundaryBuildMaterials.add(configMaterialBoundary4)
        portalBoundaryBuildMaterials.add(configMaterialBoundary5)
        portalBoundaryBuildMaterials.add(configMaterialOrigin)
    }

    fun maxDimX(plane: Plane): Int {
        return if (plane.x()) configAreaMaxWidth else 1
    }

    fun maxDimY(plane: Plane): Int {
        return if (plane.y()) configAreaMaxHeight else 1
    }

    fun maxDimZ(plane: Plane): Int {
        return if (plane.z()) configAreaMaxWidth else 1
    }

    private fun rememberNewConsole(player: Player, consoleBlock: Block): Boolean {
        val changed = consoleBlock != pendingConsole[player.uniqueId]
        // Add consoleBlock as pending console
        pendingConsole[player.uniqueId] = consoleBlock
        if (changed) {
            langSelectBoundaryNow!!.send(player)
        }
        return changed
    }

    // Wrapper that performs an existence/permission check (checkOnly = true)
    private fun canLinkConsoleCheck(player: Player, boundary: PortalBoundary, console: Block): Boolean {
        return canLinkConsole(player, boundary.allBlocks(), console, null, true)
    }

    // Wrapper that links an existing portal (checkOnly = false)
    private fun canLinkConsoleLink(player: Player, portal: Portal, console: Block): Boolean {
        // Gather all portal blocks that aren't consoles
        val blocksNonNull: List<Block> = portal
            .blocks()!!
            .stream()
            .filter { pb: PortalBlock? -> pb!!.type() != PortalBlock.Type.CONSOLE }
            .map<Block> { pb: PortalBlock? -> pb!!.block() }
            .collect(Collectors.toList())
        val blocks: MutableList<Block> = ArrayList(blocksNonNull)
        return canLinkConsole(player, blocks, console, portal, false)
    }

    private fun canLinkConsole(
        player: Player,
        blocks: MutableList<Block>,
        console: Block,
        existingPortal: Portal?,
        checkOnly: Boolean
    ): Boolean {
        // Check a console block type
        if (console.type != configMaterialConsole) {
            langConsoleInvalidType!!.send(player)
            return false
        }

        // Check world
        if (console.world != blocks[0].world) {
            langConsoleDifferentWorld!!.send(player)
            return false
        }

        // Check distance
        var foundValidBlock = false
        for (block in blocks) {
            if (abs(console.x - block.x) <= configConsoleMaxDistanceXz && abs(console.y - block.y) <= configConsoleMaxDistanceY && abs(
                    console.z - block.z
                ) <= configConsoleMaxDistanceXz
            ) {
                foundValidBlock = true
                break
            }
        }

        if (!foundValidBlock) {
            langConsoleTooFarAway!!.send(player)
            return false
        }

        // Call event: PortalLinkConsoleEvent expects MutableList<Block?>?, so create a nullable list copy
        val eventBlocks: MutableList<Block?> = ArrayList()
        for (b in blocks) eventBlocks.add(b)
        val event = PortalLinkConsoleEvent(player, console, eventBlocks, checkOnly, existingPortal)
        module!!.server.pluginManager.callEvent(event)
        if (event.isCancelled() && !player.hasPermission(module!!.adminPermission)) {
            langLinkRestricted!!.send(player)
            return false
        }

        return true
    }

    private fun linkConsole(player: Player, console: Block, portal: Portal): Boolean {
        if (!canLinkConsoleLink(player, portal, console)) {
            return false
        }

        // Add portal block
        module!!.addNewPortalBlock(portal, createPortalBlock(console))

        // Update block blocks
        portal.updateBlocks(module!!)
        return true
    }

    private fun findBoundary(player: Player?, block: Block): PortalBoundary? {
        val boundary = searchAt(this, block)
        if (boundary == null) {
            langNoBoundaryFound!!.send(player)
            return null
        }

        // Check for error
        when (boundary.errorState()) {
            ErrorState.NONE -> {}
            ErrorState.NO_ORIGIN -> {
                langNoOrigin!!.send(player)
                return null
            }

            ErrorState.MULTIPLE_ORIGINS -> {
                langMultipleOrigins!!.send(player)
                return null
            }

            ErrorState.NO_PORTAL_BLOCK_ABOVE_ORIGIN -> {
                langNoPortalBlockAboveOrigin!!.send(player)
                return null
            }

            ErrorState.NOT_ENOUGH_PORTAL_BLOCKS_ABOVE_ORIGIN -> {
                langNotEnoughPortalBlocksAboveOrigin!!.send(player)
                return null
            }

            ErrorState.TOO_LARGE_X -> {
                langTooLarge!!.send(player, "§6x")
                return null
            }

            ErrorState.TOO_LARGE_Y -> {
                langTooLarge!!.send(player, "§6y")
                return null
            }

            ErrorState.TOO_LARGE_Z -> {
                langTooLarge!!.send(player, "§6z")
                return null
            }

            ErrorState.TOO_SMALL_SPAWN_X -> {
                langTooSmallSpawn!!.send(player, "§6x")
                return null
            }

            ErrorState.TOO_SMALL_SPAWN_Y -> {
                langTooSmallSpawn!!.send(player, "§6y")
                return null
            }

            ErrorState.TOO_SMALL_SPAWN_Z -> {
                langTooSmallSpawn!!.send(player, "§6z")
                return null
            }

            ErrorState.PORTAL_AREA_OBSTRUCTED -> {
                langPortalAreaObstructed!!.send(player)
                return null
            }

            ErrorState.TOO_MANY_PORTAL_AREA_BLOCKS -> {
                langTooManyPortalAreaBlocks!!.send(
                    player,
                    "§6" + boundary.portalAreaBlocks()!!.size,
                    "§6$configAreaMaxBlocks"
                )
                return null
            }
        }

        if (boundary.intersectsExistingPortal(this)) {
            langIntersectsExistingPortal!!.send(player)
            return null
        }

        return boundary
    }

    fun isTypePartOfBoundary(material: Material?): Boolean {
        return (material == configMaterialBoundary1 || material == configMaterialBoundary2 || material == configMaterialBoundary3 || material == configMaterialBoundary4 || material == configMaterialBoundary5
                )
    }

    fun isTypePartOfBoundaryOrOrigin(material: Material?): Boolean {
        return material == configMaterialOrigin || isTypePartOfBoundary(material)
    }

    private fun checkConstructionConditions(
        player: Player,
        console: Block,
        boundaryBlock: Block,
        checkOnly: Boolean
    ): PortalBoundary? {
        if (module!!.isPortalBlock(boundaryBlock)) {
            module!!.log.severe(
                "constructPortal() was called on a boundary that already belongs to a portal! This is a bug."
            )
            return null
        }

        // Search for valid portal boundary
        val boundary = findBoundary(player, boundaryBlock) ?: return null

        // Check portal construct event
        val event = PortalConstructEvent(player, boundary, checkOnly)
        module!!.server.pluginManager.callEvent(event)
        if (event.isCancelled) {
            langBuildRestricted!!.send(player)
            return null
        }

        // Check console distance and build permission
        if (!canLinkConsoleCheck(player, boundary, console)) {
            return null
        }

        return boundary
    }

    private fun createPortalBlock(block: Block): PortalBlock {
        val type: PortalBlock.Type
        var mat = block.type
        // treat cave air and void air as normal air
        if (mat == Material.CAVE_AIR || mat == Material.VOID_AIR) {
            mat = Material.AIR
        }
        when (mat) {
            configMaterialConsole -> {
                type = PortalBlock.Type.CONSOLE
            }
            configMaterialBoundary1 -> {
                type = PortalBlock.Type.BOUNDARY1
            }
            configMaterialBoundary2 -> {
                type = PortalBlock.Type.BOUNDARY2
            }
            configMaterialBoundary3 -> {
                type = PortalBlock.Type.BOUNDARY3
            }
            configMaterialBoundary4 -> {
                type = PortalBlock.Type.BOUNDARY4
            }
            configMaterialBoundary5 -> {
                type = PortalBlock.Type.BOUNDARY5
            }
            configMaterialOrigin -> {
                type = PortalBlock.Type.ORIGIN
            }
            configMaterialPortalArea -> {
                type = PortalBlock.Type.PORTAL
            }
            else -> {
                module!!.log.warning(
                         "Invalid block type '" +
                                 mat +
                                 "' encountered in portal block creation. Assuming boundary variant 1."
                     )
                type = PortalBlock.Type.BOUNDARY1
            }
        }
        return PortalBlock(block, type)
    }

    private fun constructPortal(player: Player, console: Block, boundaryBlock: Block): Boolean {
        if (checkConstructionConditions(player, console, boundaryBlock, true) == null) {
            return false
        }

        // Show name chooser
        val enterNameMenu = module!!.menus?.enterNameMenu
        if (enterNameMenu == null) {
            module!!.log.warning("EnterNameMenu is not available")
            return false
        }
        enterNameMenu.create(player, Function2 { p: Player?, name: String? ->
            // Re-check conditions, as someone could have changed blocks. This
            // prevents this race condition.
            val boundary = checkConstructionConditions(p!!, console, boundaryBlock, false)
                ?: return@Function2 Menu.ClickResult.ERROR

            // Determine orientation
            val orientation = Orientation.from(
                boundary.plane()!!,
                boundary.originBlock()!!,
                console,
                player.location
            )

            // Construct portal
            val portal = Portal(p.uniqueId, orientation, boundary.spawn()!!)
            portal.name(name)
            module!!.addNewPortal(portal)

            // Add portal blocks
            for (block in boundary.allBlocks()) {
                module!!.addNewPortalBlock(portal, createPortalBlock(block))
            }

            // Link console
            linkConsole(p, console, portal)

            // Force update storage now, as a precaution.
            module!!.updatePersistentData()

            // Update portal blocks once
            portal.updateBlocks(module!!)
             Menu.ClickResult.SUCCESS
         })
         .open(player)

        return true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteractConsole(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val block = event.clickedBlock ?: return
        if (block.type != configMaterialConsole) {
            return
        }

        // Abort if the console belongs to another portal already.
        if (module!!.isPortalBlock(block)) {
            return
        }

        // TODO portal stone as item instead of shifting?
        // Only if player sneak-right-clicks the console
        val player = event.getPlayer()
        if (!player.isSneaking || event.hand != EquipmentSlot.HAND) {
            return
        }

        rememberNewConsole(player, block)
        PlayerUtil.swingArm(player, event.hand!!)
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    fun onPlayerInteractBoundary(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val block = event.clickedBlock ?: return
        val portal: Portal? = module!!.portalFor(block)
        val type = block.type
        if (portal == null && !portalBoundaryBuildMaterials.contains(type)) {
            return
        }

        // Break if no console is pending
        val player = event.getPlayer()
        val console = pendingConsole.remove(player.uniqueId) ?: return

        if (portal == null) {
            if (constructPortal(player, console, block)) {
                PlayerUtil.swingArm(player, event.hand!!)
            }
        } else {
            if (linkConsole(player, console, portal)) {
                PlayerUtil.swingArm(player, event.hand!!)
            }
        }

        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)
    }
}
