package org.oddlama.vane.regions

import com.google.common.collect.Sets
import net.minecraft.core.BlockPos
import org.bukkit.*
import org.bukkit.Particle.DustOptions
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.json.JSONObject
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigMaterial
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.core.persistent.storedUuidsByPrefix
import org.oddlama.vane.regions.event.RegionEnvironmentSettingEnforcer
import org.oddlama.vane.regions.event.RegionRoleSettingEnforcer
import org.oddlama.vane.regions.event.RegionSelectionListener
import org.oddlama.vane.regions.menu.RegionGroupMenuTag
import org.oddlama.vane.regions.menu.RegionMenuGroup
import org.oddlama.vane.regions.menu.RegionMenuTag
import org.oddlama.vane.regions.region.*
import org.oddlama.vane.regions.region.Role.RoleType
import org.oddlama.vane.util.PlayerUtil
import org.oddlama.vane.util.StorageUtil
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import java.util.logging.Level
import java.util.stream.Collectors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@VaneModule(name = "regions", bstats = 8643, configVersion = 4, langVersion = 3, storageVersion = 1)
class Regions : Module<Regions?>() {
    @ConfigInt(def = 4, min = 1, desc = "Minimum region extent in x direction.")
    var configMinRegionExtentX: Int = 0

    @ConfigInt(def = 4, min = 1, desc = "Minimum region extent in y direction.")
    var configMinRegionExtentY: Int = 0

    @ConfigInt(def = 4, min = 1, desc = "Minimum region extent in z direction.")
    var configMinRegionExtentZ: Int = 0

    @ConfigInt(def = 2048, min = 1, desc = "Maximum region extent in x direction.")
    var configMaxRegionExtentX: Int = 0

    @ConfigInt(def = 2048, min = 1, desc = "Maximum region extent in y direction.")
    var configMaxRegionExtentY: Int = 0

    @ConfigInt(def = 2048, min = 1, desc = "Maximum region extent in z direction.")
    var configMaxRegionExtentZ: Int = 0

    @ConfigBoolean(def = false, desc = "Use economy via VaultAPI as currency provider.")
    var configEconomyAsCurrency: Boolean = false

    @ConfigBoolean(
        def = false,
        desc = "Enable this to prevent players without the container permission from being able to view chests."
    )
    var configProhibitViewingContainers: Boolean = false

    @ConfigInt(
        def = 0,
        min = -1,
        desc = "The amount of decimal places the costs will be rounded to. If set to -1, it will round to the amount of decimal places specified by your economy plugin. If set to 0, costs will simply be rounded up to the nearest integer."
    )
    var configEconomyDecimalPlaces: Int = 0

    @ConfigMaterial(
        def = Material.DIAMOND,
        desc = "The currency material for regions. The alternative option to an economy plugin."
    )
    var configCurrency: Material? = null

    @ConfigDouble(
        def = 2.0,
        min = 0.0,
        desc = "The base amount of currency required to buy an area equal to one chunk (256 blocks)."
    )
    var configCostXzBase: Double = 0.0

    @ConfigDouble(
        def = 1.15,
        min = 1.0,
        desc = "The multiplicator determines how much the cost increases for each additional 16 blocks of height. A region of height h will cost multiplicator^(h / 16.0) * base_amount. Rounding is applied at the end."
    )
    var configCostYMultiplicator: Double = 0.0

    // Primary storage for all regions (region.id → region)
    @Persistent
    private val storageRegions: MutableMap<UUID?, Region> = HashMap<UUID?, Region>()

    private val regions: MutableMap<UUID?, Region?> = HashMap<UUID?, Region?>()

    // Primary storage for all region_groups (regionGroup.id → regionGroup)
    @Persistent
    private val storageRegionGroups: MutableMap<UUID?, RegionGroup> = HashMap<UUID?, RegionGroup>()

    // Primary storage for the default region groups for new regions created by a player
    // (player_uuid → regionGroup.id)
    @Persistent
    private val storageDefaultRegionGroup: MutableMap<UUID?, UUID?> = HashMap<UUID?, UUID?>()

    // Per-chunk lookup cache (worldId → chunk_key → [possible regions])
    private val regionsInChunkInWorld: MutableMap<UUID?, MutableMap<Long?, MutableList<Region>>> =
        HashMap<UUID?, MutableMap<Long?, MutableList<Region>>>()

    // A map containing the current extent for each player who is currently selecting a region
    // No key → Player not in selection mode
    // extent.min or extent.max null → Selection mode active, but no selection has been made yet
    private val regionSelections: MutableMap<UUID, RegionSelection> = HashMap<UUID, RegionSelection>()

    @LangMessage
    var langStartRegionSelection: TranslatedMessage? = null

    // This permission allows players (usually admins) to always administrate
    // any region (rename, delete), regardless of whether other restrictions
    // would block access.
    val adminPermission: Permission

    var menus: RegionMenuGroup? = RegionMenuGroup(this)

    var dynmapLayer: RegionDynmapLayer
    var blueMapLayer: RegionBlueMapLayer

    var economy: RegionEconomyDelegate? = null
    var vanePortalsAvailable: Boolean = false

    fun delayedOnEnable() {
        if (configEconomyAsCurrency) {
            if (!setupEconomy()) {
                configEconomyAsCurrency = false
            }
        }
    }

    private fun setupEconomy(): Boolean {
        module!!.log.info("Enabling economy integration")

        val vaultApiPlugin: Plugin? = module!!.server.pluginManager.getPlugin("Vault")
        if (vaultApiPlugin == null) {
            module!!.log.severe(
                "Economy was selected as the currency provider, but the Vault plugin wasn't found! Falling back to material currency."
            )
            return false
        }

        economy = RegionEconomyDelegate(this)
        return economy!!.setup(vaultApiPlugin)
    }

    override fun onModuleEnable() {
        val portalsPlugin: Plugin? = module!!.server.pluginManager.getPlugin("vane-portals")
        if (portalsPlugin != null) {
            RegionPortalIntegration(this, portalsPlugin)
            vanePortalsAvailable = true
        }

        scheduleNextTick { this.delayedOnEnable() }
        // Every second: Visualize selections
        scheduleTaskTimer({ this.visualizeSelections() }, 1L, 20L)
    }

    fun allRegions(): MutableCollection<Region?> {
        return regions
            .values
            .stream()
            .filter { p: Region? -> server.getWorld(p!!.extent()!!.world()!!) != null }
            .collect(Collectors.toList())
    }

    fun allRegionGroups(): MutableCollection<RegionGroup?> {
        // storageRegionGroups.values tiene tipo MutableCollection<RegionGroup> (no-nulos),
        // pero los callsites esperan MutableCollection<RegionGroup?> (elementos nullable).
        // Mapear a una lista mutable de elementos nullable satisface la invariancia.
        return storageRegionGroups.values.map { it as RegionGroup? }.toMutableList()
    }

    fun startRegionSelection(player: Player) {
        regionSelections[player.uniqueId] = RegionSelection(this)
        langStartRegionSelection!!.send(player)
    }

    fun cancelRegionSelection(player: Player) {
        regionSelections.remove(player.uniqueId)
    }

    fun isSelectingRegion(player: Player): Boolean {
        return regionSelections.containsKey(player.uniqueId)
    }

    fun getRegionSelection(player: Player): RegionSelection {
        return regionSelections[player.uniqueId]!!
    }

    private fun visualizeEdge(world: World, c1: BlockPos, c2: BlockPos, valid: Boolean) {
        // Unfortunately, particle spawns are normally distributed.
        // To still have a good visualization, we need to calculate a stddev that looks
        // good.
        // Empirically, we chose a 1/2 of the radius.
        val mx = (c1.getX() + c2.getX()) / 2.0 + 0.5
        val my = (c1.getY() + c2.getY()) / 2.0 + 0.5
        val mz = (c1.getZ() + c2.getZ()) / 2.0 + 0.5
        var dx = abs(c1.getX() - c2.getX()).toDouble()
        var dy = abs(c1.getY() - c2.getY()).toDouble()
        var dz = abs(c1.getZ() - c2.getZ()).toDouble()
        val len = dx + dy + dz
        val count = min(VISUALIZE_MAX_PARTICLES, (VISUALIZE_PARTICLES_PER_BLOCK * len).toInt())

        // Compensate for using normal distributed particles
        dx *= VISUALIZE_STDDEV_COMPENSATION
        dy *= VISUALIZE_STDDEV_COMPENSATION
        dz *= VISUALIZE_STDDEV_COMPENSATION

        // Spawn base particles
        world.spawnParticle<Any?>(
            Particle.END_ROD,
            mx,
            my,
            mz,
            count,
            dx,
            dy,
            dz,
            0.0,  // speed
            null,  // data
            true
        ) // force

        // Spawn colored particles indicating validity
        world.spawnParticle<DustOptions?>(
            Particle.DUST,
            mx,
            my,
            mz,
            count,
            dx,
            dy,
            dz,
            0.0,  // speed
            if (valid) VISUALIZE_DUST_VALID else VISUALIZE_DUST_INVALID,  // data
            true
        ) // force
    }

    private fun visualizeFace(world: World, c1: BlockPos, c2: BlockPos, c3: BlockPos, c4: BlockPos, valid: Boolean) {
        visualizeEdge(world, c1, c2, valid)
        visualizeEdge(world, c2, c3, valid)
        visualizeEdge(world, c3, c4, valid)
        visualizeEdge(world, c4, c1, valid)
    }

    private fun visualizeSelections() {
        for (selectionOwner in regionSelections.keys) {
            val selection = regionSelections[selectionOwner] ?: continue

            // Get player for selection
            val offlinePlayer = server.getOfflinePlayer(selectionOwner)
            if (!offlinePlayer.isOnline) {
                continue
            }
            val player = offlinePlayer.player

            // Both blocks are set
            if (selection.primary == null || selection.secondary == null) {
                continue
            }

            // World match
            if (selection.primary!!.world != selection.secondary!!.world) {
                continue
            }

            // Extent can be visualized. Prepare parameters.
            val world = selection.primary!!.world
            // Check if selection is valid
            val valid = selection.isValid(player!!)

            val lx = min(selection.primary!!.x, selection.secondary!!.x)
            val ly = min(selection.primary!!.y, selection.secondary!!.y)
            val lz = min(selection.primary!!.z, selection.secondary!!.z)
            val hx = max(selection.primary!!.x, selection.secondary!!.x)
            val hy = max(selection.primary!!.y, selection.secondary!!.y)
            val hz = max(selection.primary!!.z, selection.secondary!!.z)

            // Corners
            val a = BlockPos(lx, ly, lz)
            val b = BlockPos(hx, ly, lz)
            val c = BlockPos(hx, hy, lz)
            val d = BlockPos(lx, hy, lz)
            val e = BlockPos(lx, ly, hz)
            val f = BlockPos(hx, ly, hz)
            val g = BlockPos(hx, hy, hz)
            val h = BlockPos(lx, hy, hz)

            // Visualize each edge
            visualizeFace(world, a, b, c, d, valid)
            visualizeFace(world, e, f, g, h, valid)
            visualizeEdge(world, a, e, valid)
            visualizeEdge(world, b, f, valid)
            visualizeEdge(world, c, g, valid)
            visualizeEdge(world, d, h, valid)
        }
    }

    fun addRegionGroup(group: RegionGroup) {
        storageRegionGroups[group.id()] = group
        markPersistentStorageDirty()
    }

    fun canRemoveRegionGroup(group: RegionGroup): Boolean {
        // Returns true if this region group is unused and can be removed.

        // If this region group is the fallback default group, it is permanent!

        if (storageDefaultRegionGroup.containsValue(group.id())) {
            return false
        }

        // If any region uses this group, we can't remove it.
        return regions.values.stream().noneMatch { r: Region? -> r!!.regionGroupId() == group.id() }
    }

    fun removeRegionGroup(group: RegionGroup) {
        // Assert that this region group is unused.
        if (!canRemoveRegionGroup(group)) {
            return
        }

        // Remove a region group from storage
        if (storageRegionGroups.remove(group.id()) == null) {
            // Was already removed
            return
        }

        markPersistentStorageDirty()

        // Close and taint all related open menus
        module!!.core!!.menuManager!!.forEachOpen { player: Player?, menu: Menu? ->
            if (menu!!.tag() is RegionGroupMenuTag &&
                (menu.tag() as RegionGroupMenuTag).regionGroupId() == group.id()
            ) {
                menu.taint()
                menu.close(player!!)
            }
        }
    }

    fun getRegionGroup(regionGroup: UUID?): RegionGroup {
        return storageRegionGroups[regionGroup]!!
    }

    fun createRegionFromSelection(player: Player, name: String?): Boolean {
        val selection = getRegionSelection(player)
        if (!selection.isValid(player)) {
            return false
        }

        // Take currency items / withdraw economy
        val price = selection.price()
        if (configEconomyAsCurrency) {
            if (price > 0) {
                val transaction = economy!!.withdraw(player, price)
                if (!transaction!!.transactionSuccess()) {
                    log.warning(
                        "Player " +
                                player +
                                " tried to create region '" +
                                name +
                                "' (cost " +
                                price +
                                ") but the economy plugin failed to withdraw:"
                    )
                    log.warning("Error message: " + transaction.errorMessage)
                    return false
                }
            }
        } else {
            val map: MutableMap<ItemStack?, Int> = HashMap()
            map[ItemStack(configCurrency!!)] = price.toInt()
            if (price > 0 && !PlayerUtil.takeItems(player, map)) {
                return false
            }
        }

        val defRegionGroup = getOrCreateDefaultRegionGroup(player)
        val region = Region(name, player.uniqueId, selection.extent(), defRegionGroup.id())
        addNewRegion(region)
        cancelRegionSelection(player)
        return true
    }

    fun currencyString(): String? {
        return if (configEconomyAsCurrency) {
            economy!!.currencyNamePlural()
        } else {
            configCurrency.toString().lowercase(Locale.getDefault())
        }
    }

    fun addNewRegion(region: Region) {
        region.invalidated = true
        // Index region for fast lookup
        indexRegion(region)
    }

    fun removeRegion(region: Region) {
        // Remove region from storage
        if (regions.remove(region.id()) == null) {
            // Was already removed
            return
        }

        // Force update storage now, as a precaution.
        updatePersistentData()

        // Close and taint all related open menus
        module!!.core!!.menuManager!!.forEachOpen { player: Player?, menu: Menu? ->
            if (menu!!.tag() is RegionMenuTag &&
                (menu.tag() as RegionMenuTag).regionId() == region.id()
            ) {
                menu.taint()
                menu.close(player!!)
            }
        }

        // Remove a region from index
        indexRemoveRegion(region)

        // Remove map marker
        removeMarker(region.id()!!)
    }

    fun updateMarker(region: Region) {
        dynmapLayer.updateMarker(region)
        blueMapLayer.updateMarker(region)
    }

    fun removeMarker(regionId: UUID) {
        dynmapLayer.removeMarker(regionId)
        blueMapLayer.removeMarker(regionId)
    }

    private fun indexRegion(region: Region) {
        regions[region.id()] = region

        // Adds the region to the lookup map at all intersecting chunks
        val min = region.extent()!!.min()
        val max = region.extent()!!.max()

        val worldId = min!!.world.uid
        val regionsInChunk =
            regionsInChunkInWorld.computeIfAbsent(worldId) { _ -> HashMap<Long?, MutableList<Region>>() }

        val minChunk = min.chunk
        val maxChunk = max!!.chunk

        // Iterate all the chunks which intersect the region
        for (cx in minChunk.x..maxChunk.x) {
            for (cz in minChunk.z..maxChunk.z) {
                val chunkKey = Chunk.getChunkKey(cx, cz)
                val possibleRegions = regionsInChunk.computeIfAbsent(chunkKey) { _ -> ArrayList<Region>() }
                possibleRegions.add(region)
            }
        }

        // Create map marker
        updateMarker(region)
    }

    private fun indexRemoveRegion(region: Region) {
        // Removes the region from the lookup map at all intersecting chunks
        val min = region.extent()!!.min()
        val max = region.extent()!!.max()

        val worldId = min!!.world.uid
        val regionsInChunk = regionsInChunkInWorld[worldId] ?: return

        val minChunk = min.chunk
        val maxChunk = max!!.chunk

        // Iterate all the chunks which intersect the region
        for (cx in minChunk.x..maxChunk.x) {
            for (cz in minChunk.z..maxChunk.z) {
                val chunkKey = Chunk.getChunkKey(cx, cz)
                val possibleRegions = regionsInChunk[chunkKey] ?: continue
                possibleRegions.remove(region)
            }
        }
    }

    private fun findRegionInChunk(worldId: UUID, chunkKey: Long, inside: (Region) -> Boolean): Region? {
        val regionsInChunk = regionsInChunkInWorld[worldId] ?: return null
        val possibleRegions = regionsInChunk[chunkKey] ?: return null
        return possibleRegions.firstOrNull { inside(it) }
    }

    fun regionAt(loc: Location): Region? =
        findRegionInChunk(loc.world.uid, loc.chunk.chunkKey) { it.extent()!!.isInside(loc) }

    fun regionAt(block: Block): Region? =
        findRegionInChunk(block.world.uid, block.chunk.chunkKey) { it.extent()!!.isInside(block) }

    fun mayAdministrate(player: Player, group: RegionGroup): Boolean {
        return (player.uniqueId == group.owner() || group.getRole(player.uniqueId)!!.getSetting(RoleSetting.ADMIN))
    }

    fun mayAdministrate(player: Player, region: Region): Boolean {
        return player.uniqueId == region.owner() || player.hasPermission(adminPermission)
    }

    fun getOrCreateDefaultRegionGroup(owner: Player): RegionGroup {
        val ownerId = owner.uniqueId
        val regionGroupId = storageDefaultRegionGroup[ownerId]
        if (regionGroupId != null) {
            return getRegionGroup(regionGroupId)
        }

        // Create and save owner's default group
        val regionGroup = RegionGroup("[default] " + owner.name, ownerId)
        addRegionGroup(regionGroup)

        // Set group as the default
        storageDefaultRegionGroup[ownerId] = regionGroup.id()
        markPersistentStorageDirty()

        return regionGroup
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // Remove pending selection
        cancelRegionSelection(event.getPlayer())
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

    init {
        roleOverrides = RegionGlobalRoleOverrides(this)
        environmentOverrides = RegionGlobalEnvironmentOverrides(this)

        org.oddlama.vane.regions.commands.Region(this)

        RegionEnvironmentSettingEnforcer(this)
        RegionRoleSettingEnforcer(this)

        RegionSelectionListener(this)
        dynmapLayer = RegionDynmapLayer(this)
        blueMapLayer = RegionBlueMapLayer(this)

        // Register admin permission
        adminPermission = Permission(
            "vane." + module!!.annotationName + ".admin",
            "Allows administration of any region",
            PermissionDefault.OP
        )
        module!!.registerPermission(adminPermission)
    }

    private fun storedRegionIds(data: org.bukkit.persistence.PersistentDataContainer, prefix: String): Set<UUID> =
        data.storedUuidsByPrefix(prefix)

    fun loadPersistentData(world: World) {
        val data = world.persistentDataContainer
        val storageRegionPrefix = "$STORAGE_REGIONS."

        // Load all currently stored regions.
        val pdcRegions = storedRegionIds(data, storageRegionPrefix)

        for (regionId in pdcRegions) {
            val key = NamespacedKey.fromString(storageRegionPrefix + regionId.toString()) ?: continue
            val jsonBytes: ByteArray? = data.get(key, PersistentDataType.BYTE_ARRAY)
            try {
                val region: Region? = PersistentSerializer.fromJson(Region::class.java, JSONObject(String(jsonBytes!!)))
                indexRegion(region!!)
            } catch (e: IOException) {
                log.log(Level.SEVERE, "error while serializing persistent data!", e)
            }
        }
        log.log(
            Level.INFO,
            "Loaded " + pdcRegions.size + " regions for world " + world.name + "(" + world.uid + ")"
        )

        // Convert regions from legacy storage
        val removeFromLegacyStorage: MutableSet<UUID?> = HashSet<UUID?>()
        var converted = 0
        for (region in storageRegions.values) {
            if (region.extent()!!.world() != world.uid) {
                continue
            }

            if (regions.containsKey(region.id())) {
                removeFromLegacyStorage.add(region.id())
                continue
            }

            indexRegion(region)
            region.invalidated = true
            converted += 1
        }

        // Remove any region that was successfully loaded from the new storage.
        removeFromLegacyStorage.forEach(Consumer { key: UUID? -> storageRegions.remove(key) })
        if (removeFromLegacyStorage.isNotEmpty()) {
            markPersistentStorageDirty()
        }

        // Save if we had any conversions
        if (converted > 0) {
            updatePersistentData()
        }
    }

    fun updatePersistentData() {
        for (world in server.worlds) {
            updatePersistentData(world)
        }
    }

    fun updatePersistentData(world: World) {
        val data = world.persistentDataContainer
        val storageRegionPrefix = "$STORAGE_REGIONS."

        // Update invalidated regions
        regions
            .values
            .stream()
            .filter { x: Region? -> x!!.invalidated && x.extent()!!.world() == world.uid }
            .forEach { region: Region? ->
                try {
                    val json = PersistentSerializer.toJson(Region::class.java, region)
                    data.set(
                        NamespacedKey.fromString(storageRegionPrefix + region!!.id().toString())!!,
                        PersistentDataType.BYTE_ARRAY,
                        json.toString().toByteArray()
                    )
                } catch (e: IOException) {
                    log.log(Level.SEVERE, "error while serializing persistent data!", e)
                    return@forEach
                }
                region.invalidated = false
            }

        // Get all currently stored regions.
        val storedRegions = storedRegionIds(data, storageRegionPrefix)

        // Remove all regions that no longer exist
        Sets.difference<UUID?>(storedRegions, regions.keys)
            .forEach(Consumer { id: UUID? -> data.remove(NamespacedKey.fromString(storageRegionPrefix + id.toString())!!) }
            )
    }

    override fun onModuleDisable() {
        // Save data
        updatePersistentData()
        super.onModuleDisable()
    }

    companion object {
        //
        //                                                  ┌───────────────────────┐
        // ┌────────────┐  is   ┌───────────────┐         ┌───────────────────────┐ |  belongs to
        // ┌─────────────────┐
        // |  Player 1  | ────> | [Role] Admin  | ───┬──> | [RegionGroup] Default |─┘ <───┬─────── |
        // [Region] MyHome |
        // └────────────┘       └───────────────┘    |    └───────────────────────┘       |
        // └─────────────────┘
        //                                           |                                    |
        // ┌────────────┐  in   ┌───────────────┐    |                                    |
        // ┌─────────────────────┐
        // |  Player 2  | ────> | [Role] Friend | ───┤ (are roles of)                     └─────── |
        // [Region] Drecksloch |
        // └────────────┘       └───────────────┘    |
        // └─────────────────────┘
        //                                           |
        // ┌────────────┐  in   ┌───────────────┐    |
        // | Any Player | ────> | [Role] Others | ───┘
        // └────────────┘       └───────────────┘
        // Add (de-)serializers
        init {
            PersistentSerializer.serializers[EnvironmentSetting::class.java] = PersistentSerializer.Function { x: Any? -> (x as EnvironmentSetting).name }
            PersistentSerializer.deserializers[EnvironmentSetting::class.java] = PersistentSerializer.Function { x: Any? -> EnvironmentSetting.valueOf((x as String?)!!) }
            PersistentSerializer.serializers[RoleSetting::class.java] = PersistentSerializer.Function { x: Any? -> (x as RoleSetting).name }
            PersistentSerializer.deserializers[RoleSetting::class.java] = PersistentSerializer.Function { x: Any? -> RoleSetting.valueOf((x as String?)!!) }
            PersistentSerializer.serializers[Role::class.java] = PersistentSerializer.Function { obj: Any? -> Role.serialize(obj!!) }
            PersistentSerializer.deserializers[Role::class.java] = PersistentSerializer.Function { obj: Any? -> Role.deserialize(obj!!) }
            PersistentSerializer.serializers[RoleType::class.java] = PersistentSerializer.Function { x: Any? -> (x as RoleType).name }
            PersistentSerializer.deserializers[RoleType::class.java] = PersistentSerializer.Function { x: Any? -> RoleType.valueOf((x as String?)!!) }
            PersistentSerializer.serializers[RegionGroup::class.java] = PersistentSerializer.Function { obj: Any? -> RegionGroup.serialize(obj!!) }
            PersistentSerializer.deserializers[RegionGroup::class.java] = PersistentSerializer.Function { obj: Any? -> RegionGroup.deserialize(obj!!) }
            PersistentSerializer.serializers[Region::class.java] = PersistentSerializer.Function { obj: Any? -> Region.serialize(obj!!) }
            PersistentSerializer.deserializers[Region::class.java] = PersistentSerializer.Function { obj: Any? -> Region.deserialize(obj!!) }
            PersistentSerializer.serializers[RegionExtent::class.java] = PersistentSerializer.Function { obj: Any? -> RegionExtent.serialize(obj!!) }
            PersistentSerializer.deserializers[RegionExtent::class.java] = PersistentSerializer.Function { obj: Any? -> RegionExtent.deserialize(obj!!) }
        }

        var roleOverrides: RegionGlobalRoleOverrides? = null
        var environmentOverrides: RegionGlobalEnvironmentOverrides? = null

        private const val VISUALIZE_MAX_PARTICLES = 20000
        private const val VISUALIZE_PARTICLES_PER_BLOCK = 12
        private const val VISUALIZE_STDDEV_COMPENSATION = 0.25
        private val VISUALIZE_DUST_INVALID = DustOptions(Color.fromRGB(230, 60, 11), 1.0f)
        private val VISUALIZE_DUST_VALID = DustOptions(Color.fromRGB(120, 220, 60), 1.0f)

        val STORAGE_REGIONS: NamespacedKey = StorageUtil.namespacedKey("vane_regions", "regions")
    }
}
