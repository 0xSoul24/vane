package org.oddlama.vane.portals

import com.google.common.collect.Sets
import net.kyori.adventure.text.Component
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.world.*
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.*
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.functional.Consumer2
import org.oddlama.vane.core.functional.Function2
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.material.ExtendedMaterial
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.core.persistent.storedUuidsByPrefix
import org.oddlama.vane.portals.entity.FloatingItem
import org.oddlama.vane.portals.menu.PortalMenuGroup
import org.oddlama.vane.portals.menu.PortalMenuTag
import org.oddlama.vane.portals.portal.*
import org.oddlama.vane.portals.portal.Style.Companion.defaultStyle
import org.oddlama.vane.portals.portal.Style.Companion.defaultStyleKey
import org.oddlama.vane.util.*
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

@VaneModule(name = "portals", bstats = 8642, configVersion = 3, langVersion = 6, storageVersion = 2)
class Portals : Module<Portals?>() {
    @ConfigMaterialSet(
        def = [Material.PISTON, Material.STICKY_PISTON],
        desc = "Materials which may not be used to decorate portals."
    )
    var configBlacklistedMaterials: MutableSet<Material?>? = null

    // TODO better and more default styles
    @ConfigMaterialMapMapMap(
        def = [ConfigMaterialMapMapMapEntry(
            key = "vane_portals:portal_style_default",
            value = [ConfigMaterialMapMapEntry(
                key = "Active",
                value = [ConfigMaterialMapEntry(key = "Boundary1", value = Material.OBSIDIAN), ConfigMaterialMapEntry(
                    key = "Boundary2",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(
                    key = "Boundary3",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(
                    key = "Boundary4",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(
                    key = "Boundary5",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(key = "Console", value = Material.ENCHANTING_TABLE), ConfigMaterialMapEntry(
                    key = "Origin",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(key = "Portal", value = Material.END_GATEWAY)]
            ), ConfigMaterialMapMapEntry(
                key = "Inactive",
                value = [ConfigMaterialMapEntry(key = "Boundary1", value = Material.OBSIDIAN), ConfigMaterialMapEntry(
                    key = "Boundary2",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(
                    key = "Boundary3",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(
                    key = "Boundary4",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(
                    key = "Boundary5",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(key = "Console", value = Material.ENCHANTING_TABLE), ConfigMaterialMapEntry(
                    key = "Origin",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(key = "Portal", value = Material.AIR)]
            )]
        ), ConfigMaterialMapMapMapEntry(
            key = "vane_portals:portal_style_aqua",
            value = [ConfigMaterialMapMapEntry(
                key = "Active",
                value = [ConfigMaterialMapEntry(
                    key = "Boundary1",
                    value = Material.DARK_PRISMARINE
                ), ConfigMaterialMapEntry(
                    key = "Boundary2",
                    value = Material.WARPED_PLANKS
                ), ConfigMaterialMapEntry(
                    key = "Boundary3",
                    value = Material.SEA_LANTERN
                ), ConfigMaterialMapEntry(
                    key = "Boundary4",
                    value = Material.WARPED_WART_BLOCK
                ), ConfigMaterialMapEntry(
                    key = "Boundary5",
                    value = Material.LIGHT_BLUE_STAINED_GLASS
                ), ConfigMaterialMapEntry(key = "Console", value = Material.ENCHANTING_TABLE), ConfigMaterialMapEntry(
                    key = "Origin",
                    value = Material.DARK_PRISMARINE
                ), ConfigMaterialMapEntry(key = "Portal", value = Material.END_GATEWAY)]
            ), ConfigMaterialMapMapEntry(
                key = "Inactive",
                value = [ConfigMaterialMapEntry(
                    key = "Boundary1",
                    value = Material.DARK_PRISMARINE
                ), ConfigMaterialMapEntry(
                    key = "Boundary2",
                    value = Material.WARPED_PLANKS
                ), ConfigMaterialMapEntry(
                    key = "Boundary3",
                    value = Material.PRISMARINE_BRICKS
                ), ConfigMaterialMapEntry(
                    key = "Boundary4",
                    value = Material.WARPED_WART_BLOCK
                ), ConfigMaterialMapEntry(
                    key = "Boundary5",
                    value = Material.LIGHT_BLUE_STAINED_GLASS
                ), ConfigMaterialMapEntry(key = "Console", value = Material.ENCHANTING_TABLE), ConfigMaterialMapEntry(
                    key = "Origin",
                    value = Material.DARK_PRISMARINE
                ), ConfigMaterialMapEntry(key = "Portal", value = Material.AIR)]
            )]
        )],
        desc = "Portal style definitions. Must provide a material for each portal block type and activation state. The default style may be overridden."
    )
    var configStyles: MutableMap<String?, MutableMap<String?, MutableMap<String?, Material?>?>?>? = null

    @ConfigLong(
        def = 10000,
        min = 1000,
        max = 110000,
        desc = "Delay in milliseconds after which two connected portals will automatically be disabled. Purple end-gateway beams do not show up until the maximum value of 110 seconds."
    )
    var configDeactivationDelay: Long = 0

    @ConfigExtendedMaterial(
        def = "vane:decoration_end_portal_orb",
        desc = "The default portal icon. Also accepts heads from the head library."
    )
    var configDefaultIcon: ExtendedMaterial? = null

    @ConfigDouble(
        def = 0.9,
        min = 0.0,
        max = 1.0,
        desc = "Volume for the portal activation sound effect. 0 to disable."
    )
    var configVolumeActivation: Double = 0.0

    @ConfigDouble(
        def = 1.0,
        min = 0.0,
        max = 1.0,
        desc = "Volume for the portal deactivation sound effect. 0 to disable."
    )
    var configVolumeDeactivation: Double = 0.0

    @LangMessage
    var langConsoleDisplayActive: TranslatedMessage? = null

    @LangMessage
    var langConsoleDisplayInactive: TranslatedMessage? = null

    @LangMessage
    var langConsoleNoTarget: TranslatedMessage? = null

    @LangMessage
    var langUnlinkRestricted: TranslatedMessage? = null

    @LangMessage
    var langDestroyRestricted: TranslatedMessage? = null

    @LangMessage
    var langSettingsRestricted: TranslatedMessage? = null

    @LangMessage
    var langSelectTargetRestricted: TranslatedMessage? = null

    // This permission allows players (usually admins) to always modify settings
    // on any portal, regardless of whether other restrictions would block access.
    val adminPermission: Permission

    // Primary storage for all portals (portalId → portal)
    @Persistent
    private val storagePortals: MutableMap<UUID?, Portal> = HashMap<UUID?, Portal>()

    private val portals: MutableMap<UUID?, Portal> = HashMap<UUID?, Portal>()

    // Index for all portal blocks (worldId → chunk key → block key → portal block)
    private val portalBlocksInChunkInWorld: MutableMap<UUID?, MutableMap<Long?, MutableMap<Long?, PortalBlockLookup?>>> =
        HashMap<UUID?, MutableMap<Long?, MutableMap<Long?, PortalBlockLookup?>>>()

    // All loaded styles
    var styles: MutableMap<NamespacedKey?, Style> = HashMap<NamespacedKey?, Style>()

    // Cache possible area materials. This is fine as only predefined styles can
    // change this.
    var portalAreaMaterials: MutableSet<Material?> = HashSet<Material?>()

    // Track console items
    private val consoleFloatingItems: MutableMap<Block, FloatingItem?> = HashMap<Block, FloatingItem?>()

    // Connected portals (always stores both directions!)
    private val connectedPortals: MutableMap<UUID?, UUID?> = HashMap<UUID?, UUID?>()

    // Unloading ticket counter per chunk
    private val chunkTicketCount: MutableMap<Long?, Int> = HashMap<Long?, Int>()

    // Disable tasks for portals
    private val disableTasks: MutableMap<UUID?, BukkitTask?> = HashMap<UUID?, BukkitTask?>()

    var menus: PortalMenuGroup?
    var constructor: PortalConstructor
    var dynmapLayer: PortalDynmapLayer
    var blueMapLayer: PortalBlueMapLayer

    private fun registerEntities() {
        core!!.unfreezeRegistries()
        Nms.registerEntity(
            NamespacedKey.minecraft("item"),
            namespace(),
            "floating_item",
            EntityType.Builder.of({ entitytypes, world ->
                FloatingItem(entitytypes, world)
            }, MobCategory.MISC).noSave().sized(0.0f, 0.0f)
        )
    }

    override fun onConfigChange() {
        styles.clear()

        configStyles!!.forEach { (styleKey: String?, v1: MutableMap<String?, MutableMap<String?, Material?>?>?) ->
            val split: Array<String?> = styleKey!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (split.size != 2) {
                throw RuntimeException("Invalid style key: '$styleKey' is not a valid namespaced key")
            }

            val style = Style(StorageUtil.namespacedKey(split[0]!!, split[1]!!))
            v1!!.forEach { (isActive: String?, v2: MutableMap<String?, Material?>?) ->
                val active: Boolean = when (isActive) {
                    "Active" -> true
                    "Inactive" -> false
                    else -> throw RuntimeException("Invalid active state, must be either 'active' or 'inactive'")
                }
                v2!!.forEach { (portalBlockType: String?, material: Material?) ->
                    val type = PortalBlock.Type.valueOf(
                        portalBlockType!!.uppercase(
                            Locale.getDefault()
                        )
                    )
                    style.setMaterial(active, type, material!!)
                }
            }

            // Check validity and add to map.
            style.checkValid()
            styles[style.key()] = style
        }

        if (!styles.containsKey(defaultStyleKey())) {
            // Add default style if it wasn't overridden
            val defaultStyle = defaultStyle()
            styles[defaultStyle.key()] = defaultStyle
        }

        portalAreaMaterials.clear()
        // Acquire material set from styles. Will be used to speed up event checking.
        for (style in styles.values) {
            portalAreaMaterials.add(style.material(true, PortalBlock.Type.PORTAL))
        }
    }

    // Lightweight callbacks to the regions module, if it is installed.
    // Lifting the callback storage into the portals module saves us
    // from having to ship regions api with this module.
    private var isInSameRegionGroupCallback: Function2<Portal?, Portal?, Boolean?>? = null

    fun setIsInSameRegionGroupCallback(callback: Function2<Portal?, Portal?, Boolean?>?) {
        isInSameRegionGroupCallback = callback
    }

    private var playerCanUsePortalsInRegionGroupOfCallback: Function2<Player?, Portal?, Boolean?>? = null

    fun setPlayerCanUsePortalsInRegionGroupOfCallback(
        callback: Function2<Player?, Portal?, Boolean?>?
    ) {
        playerCanUsePortalsInRegionGroupOfCallback = callback
    }

    fun isInSameRegionGroup(a: Portal?, b: Portal?): Boolean {
        if (isInSameRegionGroupCallback == null) {
            return true
        }
        return isInSameRegionGroupCallback!!.apply(a, b)!!
    }

    fun playerCanUsePortalsInRegionGroupOf(player: Player?, portal: Portal?): Boolean {
        if (playerCanUsePortalsInRegionGroupOfCallback == null) {
            return true
        }
        return playerCanUsePortalsInRegionGroupOfCallback!!.apply(player, portal)!!
    }

    val isRegionsInstalled: Boolean
        get() = isInSameRegionGroupCallback != null

    fun style(key: NamespacedKey?): Style? {
        val s = styles[key]
        if (s == null) {
            logger.warning("Encountered invalid style $key, falling back to default style.")
            return styles[defaultStyleKey()]
        } else {
            return s
        }
    }

    fun removePortal(portal: Portal) {
        // Deactivate portal if needed
        val connected = connectedPortal(portal)
        if (connected != null) {
            disconnectPortals(portal, connected)
        }

        // Remove portal from storage
        if (portals.remove(portal.id()) == null) {
            // Was already removed
            return
        }

        // Remove portal blocks
        portal.blocks()!!.forEach(Consumer { portalBlock: PortalBlock? -> this.removePortalBlock(portalBlock!!) })

        // Replace references to the portal everywhere
        // and update all changed portal consoles.
        for (other in portals.values) {
            if (other.targetId() == portal.id()) {
                other.targetId(null)
                other
                    .blocks()!!
                    .stream()
                    .filter { pb: PortalBlock? -> pb!!.type() == PortalBlock.Type.CONSOLE }
                    .filter { pb: PortalBlock? -> consoleFloatingItems.containsKey(pb!!.block()) }
                    .forEach { pb: PortalBlock? -> updateConsoleItem(other, pb!!.block()!!) }
            }
        }

        // Force update storage now, as a precaution.
        updatePersistentData()

        // Close and taint all related open menus
        core!!.menuManager?.forEachOpen { player: Player?, menu: Menu? ->
            if (menu!!.tag() is PortalMenuTag &&
                (menu.tag() as PortalMenuTag).portalId() == portal.id()
            ) {
                menu.taint()
                menu.close(player!!)
            }
        }

        // Remove map marker
        removeMarker(portal.id())

        // Play sound
        portal
            .spawn()
            .getWorld()
            .playSound(portal.spawn(), Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.BLOCKS, 1.0f, 1.0f)
    }

    fun addNewPortal(portal: Portal) {
        portal.invalidated = true

        // Index the new portal
        indexPortal(portal)

        // Play sound
        portal
            .spawn()
            .getWorld()
            .playSound(portal.spawn(), Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.BLOCKS, 1.0f, 2.0f)
    }

    fun indexPortal(portal: Portal) {
        portals[portal.id()] = portal
        portal.blocks()!!.forEach(Consumer { b: PortalBlock? -> indexPortalBlock(portal, b!!) })

        // Create map marker
        updateMarker(portal)
    }

    fun allAvailablePortals(): MutableCollection<Portal?> {
        return portals.values.stream().filter { p: Portal? -> p!!.spawn().isWorldLoaded() }.collect(Collectors.toList())
    }

    fun removePortalBlock(portalBlock: PortalBlock) {
        // Restore original block
        when (portalBlock.type()) {
            PortalBlock.Type.ORIGIN -> portalBlock.block()!!.type = constructor.configMaterialOrigin!!
            PortalBlock.Type.CONSOLE -> portalBlock.block()!!.type = constructor.configMaterialConsole!!
            PortalBlock.Type.BOUNDARY1 -> portalBlock.block()!!.type = constructor.configMaterialBoundary1!!
            PortalBlock.Type.BOUNDARY2 -> portalBlock.block()!!.type = constructor.configMaterialBoundary2!!
            PortalBlock.Type.BOUNDARY3 -> portalBlock.block()!!.type = constructor.configMaterialBoundary3!!
            PortalBlock.Type.BOUNDARY4 -> portalBlock.block()!!.type = constructor.configMaterialBoundary4!!
            PortalBlock.Type.BOUNDARY5 -> portalBlock.block()!!.type = constructor.configMaterialBoundary5!!
            PortalBlock.Type.PORTAL -> portalBlock.block()!!.type = constructor.configMaterialPortalArea!!
            else -> {}
        }

        // Remove console item if a block is a console
        if (portalBlock.type() == PortalBlock.Type.CONSOLE) {
            removeConsoleItem(portalBlock.block())
        }

        // Remove from acceleration structure
        val block = portalBlock.block()
        val portalBlocksInChunk = portalBlocksInChunkInWorld[block!!.world.uid] ?: return

        val chunkKey = block.chunk.chunkKey
        val blockToPortalBlock = portalBlocksInChunk[chunkKey] ?: return

        blockToPortalBlock.remove(blockKey(block))

        // Spawn effect if not portal area
        if (portalBlock.type() != PortalBlock.Type.PORTAL) {
            portalBlock
                .block()!!
                .world
                .spawnParticle(
                    Particle.ENCHANT,
                    portalBlock.block()!!.location.add(0.5, 0.5, 0.5),
                    50,
                    0.0,
                    0.0,
                    0.0,
                    1.0
                )
        }
    }

    fun removePortalBlock(portal: Portal, portalBlock: PortalBlock) {
        // Remove from portal
        portal.blocks()!!.remove(portalBlock)

        // Remove from acceleration structure
        removePortalBlock(portalBlock)
    }

    fun addNewPortalBlock(portal: Portal, portalBlock: PortalBlock?) {
        // Add to portal
        portal.blocks()!!.add(portalBlock!!)
        portal.invalidated = true

        indexPortalBlock(portal, portalBlock)

        // Spawn effect if not portal area
        if (portalBlock.type() != PortalBlock.Type.PORTAL) {
            portalBlock
                .block()!!
                .world
                .spawnParticle(
                    Particle.PORTAL,
                    portalBlock.block()!!.location.add(0.5, 0.5, 0.5),
                    50,
                    0.0,
                    0.0,
                    0.0,
                    1.0
                )
        }
    }

    fun indexPortalBlock(portal: Portal, portalBlock: PortalBlock) {
        // Add to acceleration structure
        val block = portalBlock.block()
        val worldId = block!!.world.uid
        val portalBlocksInChunk =
            portalBlocksInChunkInWorld.computeIfAbsent(worldId) { k: UUID? -> HashMap<Long?, MutableMap<Long?, PortalBlockLookup?>>() }

        val chunkKey = block.chunk.chunkKey
        val blockToPortalBlock =
            portalBlocksInChunk.computeIfAbsent(chunkKey) { k: Long? -> HashMap<Long?, PortalBlockLookup?>() }

        blockToPortalBlock[blockKey(block)] = portalBlock.lookup(portal.id())
    }

    fun portalBlockFor(block: Block): PortalBlockLookup? {
        val portalBlocksInChunk = portalBlocksInChunkInWorld[block.world.uid] ?: return null

        val chunkKey = block.chunk.chunkKey
        val blockToPortalBlock = portalBlocksInChunk[chunkKey] ?: return null

        return blockToPortalBlock[blockKey(block)]
    }

    fun portalFor(uuid: UUID?): Portal? {
        val portal = portals[uuid]
        if (portal == null || !portal.spawn().isWorldLoaded()) {
            return null
        }
        return portal
    }

    fun portalFor(block: PortalBlockLookup): Portal {
        return portalFor(block.portalId())!!
    }

    fun portalFor(block: Block): Portal? {
        val portalBlock = portalBlockFor(block) ?: return null

        return portalFor(portalBlock)
    }

    fun isPortalBlock(block: Block): Boolean {
        val portalBlocksInChunk = portalBlocksInChunkInWorld[block.world.uid] ?: return false

        val chunkKey = block.chunk.chunkKey
        val blockToPortalBlock = portalBlocksInChunk[chunkKey] ?: return false

        return blockToPortalBlock.containsKey(blockKey(block))
    }

    fun controlledPortal(block: Block): Portal? {
        val rootPortal = portalFor(block)
        if (rootPortal != null) {
            return rootPortal
        }

        // Find adjacent console blocks in full 3x3x3 cube, which will make this block a
        // controlling block
        for (adj in BlockUtil.adjacentBlocks3D(block)) {
            val portalBlock = portalBlockFor(adj!!)
            if (portalBlock != null && portalBlock.type() == PortalBlock.Type.CONSOLE) {
                return portalFor(portalBlock)
            }
        }

        return null
    }

    fun chunksFor(portal: Portal?): MutableSet<Chunk> {
        if (portal == null) {
            return HashSet<Chunk>()
        }

        val set = HashSet<Chunk>()
        for (pb in portal.blocks()!!) {
            set.add(pb.block()!!.chunk)
        }
        return set
    }

    fun loadPortalChunks(portal: Portal?) {
        // Load chunks and adds a ticket, so they get loaded and are kept loaded
        for (chunk in chunksFor(portal)) {
            val chunkKey = chunk.chunkKey
            val ticketCounter = chunkTicketCount[chunkKey]
            if (ticketCounter == null) {
                chunk.addPluginChunkTicket(this)
                chunkTicketCount[chunkKey] = 1
            } else {
                chunkTicketCount[chunkKey] = ticketCounter + 1
            }
        }
    }

    fun allowUnloadPortalChunks(portal: Portal?) {
        // Removes the ticket so chunks can be unloaded again
        for (chunk in chunksFor(portal)) {
            val chunkKey = chunk.chunkKey
            val ticketCounter: Int = chunkTicketCount[chunkKey]!!

            if (ticketCounter > 1) {
                chunkTicketCount[chunkKey] = ticketCounter - 1
            } else if (ticketCounter == 1) {
                chunk.removePluginChunkTicket(this)
                chunkTicketCount.remove(chunkKey)
            }
        }
    }

    fun connectPortals(src: Portal, dst: Portal) {
        // Load chunks
        loadPortalChunks(src)
        loadPortalChunks(dst)

        // Add to map
        connectedPortals[src.id()] = dst.id()
        connectedPortals[dst.id()] = src.id()

        // Activate both
        src.onConnect(this, dst)
        dst.onConnect(this, src)

        // Schedule automatic disable
        startDisableTask(src, dst)
    }

    @JvmOverloads
    fun disconnectPortals(src: Portal?, dst: Portal? = portalFor(connectedPortals[src!!.id()])) {
        if (src == null || dst == null) {
            return
        }

        // Allow unloading chunks again
        allowUnloadPortalChunks(src)
        allowUnloadPortalChunks(dst)

        // Remove from a map
        connectedPortals.remove(src.id())
        connectedPortals.remove(dst.id())

        // Deactivate both
        src.onDisconnect(this, dst)
        dst.onDisconnect(this, src)

        // Reset target id's if the target portal was transient and
        // the target isn't locked.
        if (dst.visibility()!!.isTransientTarget && !src.targetLocked()) {
            src.targetId(null)
            src.updateBlocks(this)
        }

        // Remove an automatic disable task if existing
        stopDisableTask(src, dst)
    }

    private fun startDisableTask(portal: Portal, target: Portal) {
        stopDisableTask(portal, target)
        val task = scheduleTask(
            PortalDisableRunnable(portal, target),
            Conversions.msToTicks(configDeactivationDelay)
        )
        disableTasks[portal.id()] = task
        disableTasks[target.id()] = task
    }

    private fun stopDisableTask(portal: Portal, target: Portal) {
        val task1 = disableTasks.remove(portal.id())
        val task2 = disableTasks.remove(target.id())
        task1?.cancel()
        if (task2 != null && task2 !== task1) {
            task2.cancel()
        }
    }

    override fun onModuleDisable() {
        // Disable all portals now
        for (id in ArrayList<UUID?>(connectedPortals.keys)) {
            disconnectPortals(portalFor(id))
        }

        // Remove all console items, and all chunk tickets
        chunkTicketCount.clear()
        for (world in server.worlds) {
            for (chunk in world.loadedChunks) {
                // Remove console item
                forEachConsoleBlockInChunk(
                    chunk
                ) { block: Block?, console: PortalBlockLookup? -> removeConsoleItem(block) }
                // Allow chunk unloading
                chunk.removePluginChunkTicket(this)
            }
        }

        // Save data
        updatePersistentData()
        super.onModuleDisable()
    }

    fun isActivated(portal: Portal): Boolean {
        return connectedPortals.containsKey(portal.id())
    }

    fun connectedPortal(portal: Portal): Portal? {
        val connectedId = connectedPortals[portal.id()] ?: return null
        return portalFor(connectedId)
    }

    fun iconFor(portal: Portal): ItemStack? {
        val item = portal.icon()
        return item ?: configDefaultIcon!!.item()
    }

    private fun makeConsoleItem(portal: Portal, active: Boolean): ItemStack {
        val target = if (active) {
            connectedPortal(portal)
        } else {
            portal.target(this)
        }

        // Try to use target portal's block
        var item: ItemStack? = null
        if (target != null) {
            item = target.icon()
        }

        // Fallback item
        if (item == null) {
            item = configDefaultIcon!!.item()
        }

        val targetName = if (target == null) langConsoleNoTarget!!.str() else target.name()
        val displayName: Component = if (active) {
            langConsoleDisplayActive!!.format("§5$targetName")
        } else {
            langConsoleDisplayInactive!!.format("§7$targetName")
        }

        return ItemUtil.nameItem(item!!, displayName)
    }

    fun updatePortalIcon(portal: Portal) {
        // Update map marker, as name could have changed
        updateMarker(portal)

        for (activeConsole in consoleFloatingItems.keys) {
            val portalBlock = portalBlockFor(activeConsole)
            val other = portalFor(portalBlock!!)
            if (other.targetId() == portal.id()) {
                updateConsoleItem(other, activeConsole)
            }
        }
    }

    fun updatePortalVisibility(portal: Portal) {
        // Replace references to the portal everywhere if visibility
        // has changed.
        when (portal.visibility()) {
            Portal.Visibility.PRIVATE, Portal.Visibility.GROUP ->                 // Not visible from outside, these are transient.
                for (other in portals.values) {
                    if (other.targetId() == portal.id()) {
                        other.targetId(null)
                    }
                }

            Portal.Visibility.GROUP_INTERNAL ->                 // Remove from portals outside the group
                for (other in portals.values) {
                    if (other.targetId() == portal.id() && !isInSameRegionGroup(other, portal)) {
                        other.targetId(null)
                    }
                }

            else -> {}
        }

        // Update map marker
        updateMarker(portal)
    }

    fun updateConsoleItem(portal: Portal, block: Block) {
        var consoleItem = consoleFloatingItems[block]
        val isNew: Boolean
        if (consoleItem == null) {
            consoleItem = FloatingItem(
                block.world,
                block.x + 0.5,
                block.y + 1.2,
                block.z + 0.5
            )
            isNew = true
        } else {
            isNew = false
        }

        val active = isActivated(portal)
        consoleItem.setItem(Nms.itemHandle(makeConsoleItem(portal, active))!!)

        if (isNew) {
            consoleFloatingItems[block] = consoleItem
            Nms.spawn(block.world, consoleItem)
        }
    }

    fun removeConsoleItem(block: Block?) {
        val consoleItem = consoleFloatingItems.remove(block)
        consoleItem?.discard()
    }

    private fun forEachConsoleBlockInChunk(
        chunk: Chunk,
        consumer: Consumer2<Block?, PortalBlockLookup?>
    ) {
        val portalBlocksInChunk = portalBlocksInChunkInWorld[chunk.world.uid] ?: return

        val chunkKey = chunk.chunkKey
        val blockToPortalBlock = portalBlocksInChunk[chunkKey] ?: return

        blockToPortalBlock.forEach { (k: Long?, v: PortalBlockLookup?) ->
            if (v!!.type() == PortalBlock.Type.CONSOLE) {
                consumer.apply(unpackBlockKey(chunk, k!!), v)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMonitorChunkUnload(event: ChunkUnloadEvent) {
        val chunk = event.getChunk()

        // Disable all consoles in this chunk
        forEachConsoleBlockInChunk(
            chunk
        ) { block: Block?, console: PortalBlockLookup? -> removeConsoleItem(block) }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMonitorChunkLoad(event: ChunkLoadEvent) {
        val chunk = event.getChunk()

        // Enable all consoles in this chunk
        forEachConsoleBlockInChunk(chunk) { block: Block?, console: PortalBlockLookup? ->
            val portal = portalFor(console!!.portalId())
            updateConsoleItem(portal!!, block!!)
        }
    }

    fun updateMarker(portal: Portal) {
        dynmapLayer.updateMarker(portal)
        blueMapLayer.updateMarker(portal)
    }

    fun removeMarker(portalId: UUID?) {
        dynmapLayer.removeMarker(portalId)
        blueMapLayer.removeMarker(portalId)
    }

    @EventHandler
    fun onSaveWorld(event: WorldSaveEvent) {
        updatePersistentData(event.getWorld())
    }

    @EventHandler
    fun onLoadWorld(event: WorldLoadEvent) {
        loadPersistentData(event.getWorld())
    }

    @EventHandler
    fun onUnloadWorld(event: WorldUnloadEvent) {
        // Save data before unloading a world (not called on stop)
        updatePersistentData(event.getWorld())
    }

    fun updatePersistentData() {
        for (world in server.worlds) {
            updatePersistentData(world)
        }
    }

    init {
        registerEntities()

        menus = PortalMenuGroup(this)
        PortalActivator(this)
        PortalBlockProtector(this)
        constructor = PortalConstructor(this)
        PortalTeleporter(this)
        EntityMoveProcessor(this)
        dynmapLayer = PortalDynmapLayer(this)
        blueMapLayer = PortalBlueMapLayer(this)

        // Register admin permission
        adminPermission = Permission(
            "vane." + this.annotationName + ".admin",
             "Allows administration of any portal",
             PermissionDefault.OP
         )
        registerPermission(adminPermission)

        // TODO legacy, remove in v2.
        persistentStorageManager.addMigrationTo(
             2,
             "Portal visibility GROUP_INTERNAL was added. This is a no-op."
        ) { json -> }
    }

    fun loadPersistentData(world: World) {
        val data = world.persistentDataContainer
        val storagePortalPrefix = "$STORAGE_PORTALS."

        // Load all currently stored portals.
        val pdcPortals = getStoredPortalIds(data, storagePortalPrefix)

        for (portalId in pdcPortals) {
            val key = NamespacedKey.fromString(storagePortalPrefix + portalId.toString()) ?: continue
            val jsonBytes = data.get(
                key,
                PersistentDataType.BYTE_ARRAY
            )
             try {
                val portal = PersistentSerializer.fromJson(Portal::class.java, org.json.JSONObject(String(jsonBytes!!)))
                indexPortal(portal!!)
             } catch (e: IOException) {
                 logger.log(java.util.logging.Level.SEVERE, "error while serializing persistent data!", e)
             }
        }
        logger.log(
            java.util.logging.Level.INFO,
            "Loaded " + pdcPortals.size + " portals for world " + world.name + "(" + world.uid + ")"
        )

        // Convert portals from legacy storage
        val removeFromLegacyStorage: MutableSet<UUID?> = HashSet<UUID?>()
        var converted = 0
        for (portal in storagePortals.values) {
            if (portal.spawnWorld() != world.uid) {
                continue
            }

            if (portals.containsKey(portal.id())) {
                removeFromLegacyStorage.add(portal.id())
                continue
            }

            indexPortal(portal)
            portal.invalidated = true
            converted += 1
        }

        // Remove any portal that was successfully loaded from the new storage.
        removeFromLegacyStorage.forEach(Consumer { key: UUID? -> storagePortals.remove(key) })
        if (removeFromLegacyStorage.isNotEmpty()) {
            markPersistentStorageDirty()
        }

        // Update all consoles in the loaded world. These
        // might be missed by chunk load event as it runs asynchronous
        // to this function, and it can't be synchronized without annoying the server.
        for (chunk in world.loadedChunks) {
            forEachConsoleBlockInChunk(chunk) { block: Block?, console: PortalBlockLookup? ->
                val portal = portalFor(console!!.portalId())
                updateConsoleItem(portal!!, block!!)
            }
        }

        // Save if we had any conversions
        if (converted > 0) {
            updatePersistentData()
        }
    }

    fun updatePersistentData(world: World) {
        val data = world.persistentDataContainer
        val storagePortalPrefix = "$STORAGE_PORTALS."

        // Update invalidated portals
        portals
            .values
            .stream()
            .filter { x: Portal? -> x!!.invalidated && x.spawnWorld() == world.uid }
            .forEach { portal: Portal? ->
                try {
                    val json = PersistentSerializer.toJson(Portal::class.java, portal)
                    data.set(
                        NamespacedKey.fromString(storagePortalPrefix + portal!!.id().toString())!!,
                        PersistentDataType.BYTE_ARRAY,
                        json.toString().toByteArray()
                    )
                } catch (e: IOException) {
                    logger.log(java.util.logging.Level.SEVERE, "error while serializing persistent data!", e)
                    return@forEach
                }
                portal.invalidated = false
            }

        // Get all currently stored portals.
        val storedPortals = getStoredPortalIds(data, storagePortalPrefix)

        // Remove all portals that no longer exist
        Sets.difference(storedPortals, portals.keys)
            .forEach(Consumer { id: UUID? -> data.remove(NamespacedKey.fromString(storagePortalPrefix + id.toString())!!) }
            )
    }

    // Helper: extract stored portal UUIDs from a world's PersistentDataContainer
    private fun getStoredPortalIds(data: org.bukkit.persistence.PersistentDataContainer, storagePortalPrefix: String): Set<UUID> =
        data.storedUuidsByPrefix(storagePortalPrefix)

    private inner class PortalDisableRunnable(private val src: Portal?, private val dst: Portal?) : Runnable {
        override fun run() {
            this@Portals.disconnectPortals(src, dst)
        }
    }

    companion object {
        // Add (de-)serializers
        init {
            PersistentSerializer.serializers[Orientation::class.java] = PersistentSerializer.Function { x: Any? -> (x as Orientation).name }
            PersistentSerializer.deserializers[Orientation::class.java] = PersistentSerializer.Function { x: Any? -> Orientation.valueOf((x as String?)!!) }
            PersistentSerializer.serializers[Portal::class.java] = PersistentSerializer.Function { obj: Any? -> Portal.serialize(obj!!) }
            PersistentSerializer.deserializers[Portal::class.java] = PersistentSerializer.Function { obj: Any? -> Portal.deserialize(obj!!) }
             PersistentSerializer.serializers[Portal.Visibility::class.java] = PersistentSerializer.Function { x: Any? -> (x as Portal.Visibility).name }
             PersistentSerializer.deserializers[Portal.Visibility::class.java] = PersistentSerializer.Function { x: Any? -> Portal.Visibility.valueOf((x as String?)!!) }
            PersistentSerializer.serializers[PortalBlock::class.java] = PersistentSerializer.Function { obj: Any? -> PortalBlock.serialize(obj!!) }
            PersistentSerializer.deserializers[PortalBlock::class.java] = PersistentSerializer.Function { obj: Any? -> PortalBlock.deserialize(obj!!) }
             PersistentSerializer.serializers[PortalBlock.Type::class.java] = PersistentSerializer.Function { x: Any? -> (x as PortalBlock.Type).name }
             PersistentSerializer.deserializers[PortalBlock.Type::class.java] = PersistentSerializer.Function { x: Any? -> PortalBlock.Type.valueOf((x as String?)!!) }
            PersistentSerializer.serializers[PortalBlockLookup::class.java] = PersistentSerializer.Function { obj: Any? -> PortalBlockLookup.serialize(obj!!) }
            PersistentSerializer.deserializers[PortalBlockLookup::class.java] = PersistentSerializer.Function { obj: Any? -> PortalBlockLookup.deserialize(obj!!) }
            PersistentSerializer.serializers[Style::class.java] = PersistentSerializer.Function { obj: Any? -> Style.serialize(obj!!) }
            PersistentSerializer.deserializers[Style::class.java] = PersistentSerializer.Function { obj: Any? -> Style.deserialize(obj!!) }
        }

        private fun blockKey(block: Block): Long {
            return ((block.y shl 8) or ((block.x and 0xF) shl 4) or ((block.z and 0xF))).toLong()
        }

        private fun unpackBlockKey(chunk: Chunk, blockKey: Long): Block {
            val y = (blockKey shr 8).toInt()
            val x = ((blockKey shr 4) and 0xFL).toInt()
            val z = (blockKey and 0xFL).toInt()
            return chunk.getBlock(x, y, z)
        }

        val STORAGE_PORTALS: NamespacedKey = StorageUtil.namespacedKey("vane_portals", "portals")
    }
}
