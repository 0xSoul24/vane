package org.oddlama.vane.portals.portal

import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.EndGateway
import org.bukkit.block.data.FaceAttachable.AttachedFace
import org.bukkit.block.data.type.Switch
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.portals.Portals
import org.oddlama.vane.portals.event.PortalActivateEvent
import org.oddlama.vane.portals.event.PortalDeactivateEvent
import org.oddlama.vane.portals.event.PortalOpenConsoleEvent
import org.oddlama.vane.portals.portal.Style.Companion.defaultStyleKey
import org.oddlama.vane.util.BlockUtil
import org.oddlama.vane.util.LazyLocation
import java.io.IOException
import java.util.*
import kotlin.Any
import kotlin.Boolean
import kotlin.Comparator
import kotlin.Int
import kotlin.RuntimeException
import kotlin.String
import kotlin.Throws

class Portal {
    private var id: UUID? = null
    private var owner: UUID? = null
    private var orientation: Orientation? = null
    private var spawn: LazyLocation? = null
    private var blocks: MutableList<PortalBlock>? = ArrayList<PortalBlock>()

    private var name: String? = "Portal"
    private var style: NamespacedKey? = defaultStyleKey()
    private var styleOverride: Style? = null
    private var icon: ItemStack? = null
    private var visibility: Visibility? = Visibility.PRIVATE

    private var exitOrientationLocked = false
    private var targetId: UUID? = null
    private var targetLocked = false

    // Whether the portal should be saved on the next occasion.
    // Not a saved field.
    @JvmField
    var invalidated: Boolean = true

    private constructor()

    constructor(owner: UUID?, orientation: Orientation?, spawn: Location) {
        this.id = UUID.randomUUID()
        this.owner = owner
        this.orientation = orientation
        this.spawn = LazyLocation(spawn.clone())
    }

    fun id(): UUID? {
        return id
    }

    fun owner(): UUID? {
        return owner
    }

    fun orientation(): Orientation? {
        return orientation
    }

    fun spawnWorld(): UUID? {
        return spawn!!.worldId()
    }

    fun spawn(): Location {
        return spawn!!.location().clone()
    }

    fun blocks(): MutableList<PortalBlock>? {
        return blocks
    }

    fun name(): String? {
        return name
    }

    fun name(name: String?) {
        this.name = name
        this.invalidated = true
    }

    fun style(): NamespacedKey? {
        return if (styleOverride == null) style else null
    }

    fun style(style: Style) {
        if (style.key() == null) {
            this.styleOverride = style
        } else {
            this.style = style.key()
        }
        this.invalidated = true
    }

    fun icon(): ItemStack? {
        return if (icon == null) null else icon!!.clone()
    }

    fun icon(icon: ItemStack?) {
        this.icon = icon
        this.invalidated = true
    }

    fun visibility(): Visibility? {
        return visibility
    }

    fun visibility(visibility: Visibility?) {
        this.visibility = visibility
        this.invalidated = true
    }

    fun exitOrientationLocked(): Boolean {
        return exitOrientationLocked
    }

    fun exitOrientationLocked(exitOrientationLocked: Boolean) {
        this.exitOrientationLocked = exitOrientationLocked
        this.invalidated = true
    }

    fun targetId(): UUID? {
        return targetId
    }

    fun targetId(targetId: UUID?) {
        this.targetId = targetId
        this.invalidated = true
    }

    fun targetLocked(): Boolean {
        return targetLocked
    }

    fun targetLocked(targetLocked: Boolean) {
        this.targetLocked = targetLocked
        this.invalidated = true
    }

    fun portalBlockFor(block: Block?): PortalBlock? {
        for (pb in blocks()!!) {
            if (pb.block() == block) {
                return pb
            }
        }
        return null
    }

    fun target(portals: Portals): Portal? {
        return portals.portalFor(targetId())
    }

    private fun controllingBlocks(): MutableSet<Block> {
        val controllingBlocks = HashSet<Block>()
        for (pb in blocks()!!) {
            when (pb.type()) {
                PortalBlock.Type.ORIGIN, PortalBlock.Type.BOUNDARY1, PortalBlock.Type.BOUNDARY2, PortalBlock.Type.BOUNDARY3, PortalBlock.Type.BOUNDARY4, PortalBlock.Type.BOUNDARY5 -> controllingBlocks.add(
                    pb.block()!!
                )

                PortalBlock.Type.CONSOLE -> {
                    controllingBlocks.add(pb.block()!!)
                    val adj = BlockUtil.adjacentBlocks3D(pb.block()!!)
                    for (b in adj) {
                        if (b != null) controllingBlocks.add(b)
                    }
                }

                else -> {}
            }
        }
        return controllingBlocks
    }

    private fun setControllingLevers(activated: Boolean) {
        val controllingBlocks = controllingBlocks()
        val levers = HashSet<Block>()
        for (b in controllingBlocks()) {
            for (f in BlockUtil.BLOCK_FACES) {
                val l = b.getRelative(f!!)
                if (l.type != Material.LEVER) {
                    continue
                }

                val lever = l.blockData as Switch
                val attachedFace: BlockFace = when (lever.attachedFace) {
                    AttachedFace.WALL -> lever.facing.getOppositeFace()
                    AttachedFace.CEILING -> BlockFace.UP
                    AttachedFace.FLOOR -> BlockFace.DOWN
                }

                // Only when attached to a controlling block
                if (!controllingBlocks.contains(l.getRelative(attachedFace))) {
                    continue
                }

                levers.add(l)
            }
        }

        for (l in levers) {
            val lever = l.blockData as Switch
            lever.isPowered = activated
            l.blockData = lever
            BlockUtil.updateLever(l, lever.facing)
        }
    }

    fun activate(portals: Portals, player: Player?): Boolean {
        if (portals.isActivated(this)) {
            return false
        }

        val target = target(portals) ?: return false

        // Call event
        val event = PortalActivateEvent(player, this, target)
        portals.server.pluginManager.callEvent(event)
        if (event.isCancelled) {
            return false
        }

        portals.connectPortals(this, target)
        return true
    }

    fun deactivate(portals: Portals, player: Player?): Boolean {
        if (!portals.isActivated(this)) {
            return false
        }

        // Call event
        val event = PortalDeactivateEvent(player, this)
        portals.server.pluginManager.callEvent(event)
        if (event.isCancelled) {
            return false
        }

        portals.disconnectPortals(this)
        return true
    }

    fun onConnect(portals: Portals, target: Portal?) {
        // Update blocks
        updateBlocks(portals)

        // Activate all controlling levers
        setControllingLevers(true)

        val soundVolume = portals.configVolumeActivation.toFloat()
        if (soundVolume > 0.0f) {
            // Play sound
            spawn()
                .getWorld()
                .playSound(spawn(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, soundVolume, 0.8f)
        }
    }

    fun onDisconnect(portals: Portals, target: Portal?) {
        // Update blocks
        updateBlocks(portals)

        // Deactivate all controlling levers
        setControllingLevers(false)

        val soundVolume = portals.configVolumeDeactivation.toFloat()
        if (soundVolume > 0.0f) {
            // Play sound
            spawn()
                .getWorld()
                .playSound(spawn(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, soundVolume, 0.5f)
        }
    }

    fun updateBlocks(portals: Portals) {
        val curStyle: Style? = if (styleOverride == null) {
            portals.style(style)
        } else {
            styleOverride
        }

        val active = portals.isActivated(this)
        for (portalBlock in blocks!!) {
            val type = curStyle!!.material(active, portalBlock.type()!!)
            portalBlock.block()!!.type = type!!
            if (type == Material.END_GATEWAY) {
                // Disable beam
                val endGateway = portalBlock.block()!!.getState(false) as EndGateway
                endGateway.age = 200L
                endGateway.update(true, false)

                // If there's no exit location, then the game will generate a natural gateway when
                // the portal is used.
                // Setting any location will do, since the teleporting is canceled via their events
                // anyway.
                if (spawn!!.location().getWorld().environment == World.Environment.THE_END) {
                    endGateway.exitLocation = spawn!!.location()
                    endGateway.isExactTeleport = true
                }
            }
            if (portalBlock.type() == PortalBlock.Type.CONSOLE) {
                portals.updateConsoleItem(this, portalBlock.block()!!)
            }
        }
    }

    fun openConsole(portals: Portals, player: Player, console: Block?): Boolean {
        // Call event
        val event = PortalOpenConsoleEvent(player, console, this)
        portals.server.pluginManager.callEvent(event)
        if (event.isCancelled && !player.hasPermission(portals.adminPermission)) {
            return false
        }

        val consoleMenu = portals.menus?.consoleMenu
        if (consoleMenu == null) {
            portals.logger.warning("ConsoleMenu is not available")
            return false
        }
        consoleMenu.create(this, player, console).open(player)
        return true
    }

    fun copyStyle(portals: Portals, newKey: NamespacedKey?): Style {
        if (styleOverride == null) {
            val base = portals.style(style) ?: portals.style(defaultStyleKey())
                ?: throw RuntimeException("No base style available to copy")
            return base.copy(newKey)
        }
        return styleOverride!!.copy(newKey)
    }

    override fun toString(): String {
        return "Portal{id = $id, name = $name}"
    }

    enum class Visibility {
        PUBLIC,
        GROUP,
        GROUP_INTERNAL,
        PRIVATE;

        fun prev(): Visibility {
            val prev: Int = if (ordinal == 0) {
                entries.toTypedArray().size - 1
            } else {
                ordinal - 1
            }
            return entries[prev]
        }

        fun next(): Visibility {
            val next: Int = (ordinal + 1) % entries.toTypedArray().size
            return entries[next]
        }

        val isTransientTarget: Boolean
            get() = this == GROUP || this == PRIVATE

        fun requiresRegions(): Boolean {
            return this == GROUP || this == GROUP_INTERNAL
        }
    }

    class TargetSelectionComparator(player: Player) : Comparator<Portal?> {
        private val world: World = player.location.getWorld()
        private val from: Vector = player.location.toVector().setY(0.0)

        override fun compare(a: Portal?, b: Portal?): Int {
            if (a == null && b == null) return 0
            if (a == null) return 1
            if (b == null) return -1

            val aSameWorld = world == a.spawn().getWorld()
            val bSameWorld = world == b.spawn().getWorld()

            if (aSameWorld) {
                if (bSameWorld) {
                    val aDist = from.distanceSquared(a.spawn().toVector().setY(0.0))
                    val bDist = from.distanceSquared(b.spawn().toVector().setY(0.0))
                    return aDist.compareTo(bDist)
                } else {
                    return -1
                }
            } else {
                return if (bSameWorld) {
                    1
                } else {
                    a.name()!!.compareTo(b.name()!!, ignoreCase = true)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun serialize(o: Any): Any {
            val portal = o as Portal
            val json = JSONObject()
            json.put("id", PersistentSerializer.toJson(UUID::class.java, portal.id))
            json.put("owner", PersistentSerializer.toJson(UUID::class.java, portal.owner))
            json.put("orientation", PersistentSerializer.toJson(Orientation::class.java, portal.orientation))
            json.put("spawn", PersistentSerializer.toJson(LazyLocation::class.java, portal.spawn))
            try {
                json.put(
                    "blocks",
                    PersistentSerializer.toJson(Portal::class.java.getDeclaredField("blocks"), portal.blocks)
                )
            } catch (e: NoSuchFieldException) {
                throw RuntimeException("Invalid field. This is a bug.", e)
            }

            json.put("name", PersistentSerializer.toJson(String::class.java, portal.name))
            json.put("style", PersistentSerializer.toJson(NamespacedKey::class.java, portal.style))
            json.put("styleOverride", PersistentSerializer.toJson(Style::class.java, portal.styleOverride))
            json.put("icon", PersistentSerializer.toJson(ItemStack::class.java, portal.icon))
            json.put("visibility", PersistentSerializer.toJson(Visibility::class.java, portal.visibility))

            json.put(
                "exitOrientationLocked",
                PersistentSerializer.toJson(Boolean::class.javaPrimitiveType, portal.exitOrientationLocked)
            )
            json.put("targetId", PersistentSerializer.toJson(UUID::class.java, portal.targetId))
            json.put("targetLocked", PersistentSerializer.toJson(Boolean::class.javaPrimitiveType, portal.targetLocked))
            return json
        }

        @JvmStatic
        @Throws(IOException::class)
        fun deserialize(o: Any): Portal {
            val json = o as JSONObject
            val portal = Portal()
            portal.id = PersistentSerializer.fromJson(UUID::class.java, json.get("id"))
            portal.owner = PersistentSerializer.fromJson(UUID::class.java, json.get("owner"))
            portal.orientation = PersistentSerializer.fromJson(Orientation::class.java, json.get("orientation"))
            portal.spawn = PersistentSerializer.fromJson(LazyLocation::class.java, json.get("spawn"))
            try {
                @Suppress("UNCHECKED_CAST")
                portal.blocks = PersistentSerializer.fromJson(
                    Portal::class.java.getDeclaredField("blocks"),
                    json.get("blocks")
                ) as MutableList<PortalBlock>?
            } catch (e: NoSuchFieldException) {
                throw RuntimeException("Invalid field. This is a bug.", e)
            }

            portal.name = PersistentSerializer.fromJson(String::class.java, json.get("name"))
            portal.style = PersistentSerializer.fromJson(NamespacedKey::class.java, json.get("style"))
            portal.styleOverride = PersistentSerializer.fromJson(Style::class.java, json.get("styleOverride"))
            if (portal.styleOverride != null) {
                try {
                    portal.styleOverride!!.checkValid()
                } catch (_: RuntimeException) {
                    portal.styleOverride = null
                }
            }
            portal.icon = PersistentSerializer.fromJson(ItemStack::class.java, json.get("icon"))
            portal.visibility = PersistentSerializer.fromJson(Visibility::class.java, json.get("visibility"))

            portal.exitOrientationLocked = PersistentSerializer.fromJson(Boolean::class.javaPrimitiveType, json.optString("exitOrientationLocked", "false"))
                ?: false
            portal.targetId = PersistentSerializer.fromJson(UUID::class.java, json.get("targetId"))
            portal.targetLocked = PersistentSerializer.fromJson(Boolean::class.javaPrimitiveType, json.get("targetLocked"))
                ?: false
             return portal
         }
     }
 }
