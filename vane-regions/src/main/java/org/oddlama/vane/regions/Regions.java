package org.oddlama.vane.regions;

import static org.oddlama.vane.util.PlayerUtil.takeItems;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.regions.event.RegionEnvironmentSettingEnforcer;
import org.oddlama.vane.regions.event.RegionRoleSettingEnforcer;
import org.oddlama.vane.regions.event.RegionSelectionListener;
import org.oddlama.vane.regions.menu.RegionGroupMenuTag;
import org.oddlama.vane.regions.menu.RegionMenuGroup;
import org.oddlama.vane.regions.menu.RegionMenuTag;
import org.oddlama.vane.regions.region.EnvironmentSetting;
import org.oddlama.vane.regions.region.Region;
import org.oddlama.vane.regions.region.RegionExtent;
import org.oddlama.vane.regions.region.RegionGroup;
import org.oddlama.vane.regions.region.RegionSelection;
import org.oddlama.vane.regions.region.Role;
import org.oddlama.vane.regions.region.RoleSetting;
import org.oddlama.vane.util.StorageUtil;

@VaneModule(name = "regions", bstats = 8643, configVersion = 4, langVersion = 3, storageVersion = 1)
public class Regions extends Module<Regions> {
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
    static {
        PersistentSerializer.serializers.put(EnvironmentSetting.class, x -> ((EnvironmentSetting) x).name());
        PersistentSerializer.deserializers.put(EnvironmentSetting.class, x -> EnvironmentSetting.valueOf((String) x));
        PersistentSerializer.serializers.put(RoleSetting.class, x -> ((RoleSetting) x).name());
        PersistentSerializer.deserializers.put(RoleSetting.class, x -> RoleSetting.valueOf((String) x));
        PersistentSerializer.serializers.put(Role.class, Role::serialize);
        PersistentSerializer.deserializers.put(Role.class, Role::deserialize);
        PersistentSerializer.serializers.put(Role.RoleType.class, x -> ((Role.RoleType) x).name());
        PersistentSerializer.deserializers.put(Role.RoleType.class, x -> Role.RoleType.valueOf((String) x));
        PersistentSerializer.serializers.put(RegionGroup.class, RegionGroup::serialize);
        PersistentSerializer.deserializers.put(RegionGroup.class, RegionGroup::deserialize);
        PersistentSerializer.serializers.put(Region.class, Region::serialize);
        PersistentSerializer.deserializers.put(Region.class, Region::deserialize);
        PersistentSerializer.serializers.put(RegionExtent.class, RegionExtent::serialize);
        PersistentSerializer.deserializers.put(RegionExtent.class, RegionExtent::deserialize);
    }

    @ConfigInt(def = 4, min = 1, desc = "Minimum region extent in x direction.")
    public int configMinRegionExtentX;

    @ConfigInt(def = 4, min = 1, desc = "Minimum region extent in y direction.")
    public int configMinRegionExtentY;

    @ConfigInt(def = 4, min = 1, desc = "Minimum region extent in z direction.")
    public int configMinRegionExtentZ;

    @ConfigInt(def = 2048, min = 1, desc = "Maximum region extent in x direction.")
    public int configMaxRegionExtentX;

    @ConfigInt(def = 2048, min = 1, desc = "Maximum region extent in y direction.")
    public int configMaxRegionExtentY;

    @ConfigInt(def = 2048, min = 1, desc = "Maximum region extent in z direction.")
    public int configMaxRegionExtentZ;

    @ConfigBoolean(def = false, desc = "Use economy via VaultAPI as currency provider.")
    public boolean configEconomyAsCurrency;

    @ConfigBoolean(
        def = false,
        desc = "Enable this to prevent players without the container permission from being able to view chests."
    )
    public boolean configProhibitViewingContainers;

    @ConfigInt(
        def = 0,
        min = -1,
        desc = "The amount of decimal places the costs will be rounded to. If set to -1, it will round to the amount of decimal places specified by your economy plugin. If set to 0, costs will simply be rounded up to the nearest integer."
    )
    public int configEconomyDecimalPlaces;

    @ConfigMaterial(
        def = Material.DIAMOND,
        desc = "The currency material for regions. The alternative option to an economy plugin."
    )
    public Material configCurrency;

    @ConfigDouble(
        def = 2.0,
        min = 0.0,
        desc = "The base amount of currency required to buy an area equal to one chunk (256 blocks)."
    )
    public double configCostXzBase;

    @ConfigDouble(
        def = 1.15,
        min = 1.0,
        desc = "The multiplicator determines how much the cost increases for each additional 16 blocks of height. A region of height h will cost multiplicator^(h / 16.0) * base_amount. Rounding is applied at the end."
    )
    public double configCostYMultiplicator;

    // Primary storage for all regions (region.id → region)
    @Persistent
    private Map<UUID, Region> storageRegions = new HashMap<>();

    private Map<UUID, Region> regions = new HashMap<>();

    // Primary storage for all region_groups (regionGroup.id → regionGroup)
    @Persistent
    private Map<UUID, RegionGroup> storageRegionGroups = new HashMap<>();

    // Primary storage for the default region groups for new regions created by a player
    // (player_uuid → regionGroup.id)
    @Persistent
    private Map<UUID, UUID> storageDefaultRegionGroup = new HashMap<>();

    // Per-chunk lookup cache (worldId → chunk_key → [possible regions])
    private Map<UUID, Map<Long, List<Region>>> regionsInChunkInWorld = new HashMap<>();
    // A map containing the current extent for each player who is currently selecting a region
    // No key → Player not in selection mode
    // extent.min or extent.max null → Selection mode active, but no selection has been made yet
    private Map<UUID, RegionSelection> regionSelections = new HashMap<>();

    @LangMessage
    public TranslatedMessage langStartRegionSelection;

    // This permission allows players (usually admins) to always administrate
    // any region (rename, delete), regardless of whether other restrictions
    // would block access.
    public final Permission adminPermission;

    public RegionMenuGroup menus;

    public RegionDynmapLayer dynmapLayer;
    public RegionBlueMapLayer blueMapLayer;

    public RegionEconomyDelegate economy;
    public boolean vanePortalsAvailable = false;

    public static RegionGlobalRoleOverrides roleOverrides = null;
    public static RegionGlobalEnvironmentOverrides environmentOverrides = null;

    public Regions() {
        menus = new RegionMenuGroup(this);
        roleOverrides = new RegionGlobalRoleOverrides(this);
        environmentOverrides = new RegionGlobalEnvironmentOverrides(this);

        new org.oddlama.vane.regions.commands.Region(this);

        new RegionEnvironmentSettingEnforcer(this);
        new RegionRoleSettingEnforcer(this);

        new RegionSelectionListener(this);
        dynmapLayer = new RegionDynmapLayer(this);
        blueMapLayer = new RegionBlueMapLayer(this);

        // Register admin permission
        adminPermission = new Permission(
            "vane." + getModule().getAnnotationName() + ".admin",
            "Allows administration of any region",
            PermissionDefault.OP
        );
        getModule().registerPermission(adminPermission);
    }

    public void delayedOnEnable() {
        if (configEconomyAsCurrency) {
            if (!setupEconomy()) {
                configEconomyAsCurrency = false;
            }
        }
    }

    private boolean setupEconomy() {
        getModule().getLog().info("Enabling economy integration");

        Plugin vaultApiPlugin = getModule().getServer().getPluginManager().getPlugin("Vault");
        if (vaultApiPlugin == null) {
            getModule().getLog().severe(
                    "Economy was selected as the currency provider, but the Vault plugin wasn't found! Falling back to material currency."
                );
            return false;
        }

        economy = new RegionEconomyDelegate(this);
        return economy.setup(vaultApiPlugin);
    }

    @Override
    public void onModuleEnable() {
        final var portalsPlugin = getModule().getServer().getPluginManager().getPlugin("vane-portals");
        if (portalsPlugin != null) {
            new RegionPortalIntegration(this, portalsPlugin);
            vanePortalsAvailable = true;
        }

        scheduleNextTick(this::delayedOnEnable);
        // Every second: Visualize selections
        scheduleTaskTimer(this::visualizeSelections, 1l, 20l);
    }

    public Collection<Region> allRegions() {
        return regions
            .values()
            .stream()
            .filter(p -> getServer().getWorld(p.extent().world()) != null)
            .collect(Collectors.toList());
    }

    public Collection<RegionGroup> allRegionGroups() {
        return storageRegionGroups.values();
    }

    public void startRegionSelection(final Player player) {
        regionSelections.put(player.getUniqueId(), new RegionSelection(this));
        langStartRegionSelection.send(player);
    }

    public void cancelRegionSelection(final Player player) {
        regionSelections.remove(player.getUniqueId());
    }

    public boolean isSelectingRegion(final Player player) {
        return regionSelections.containsKey(player.getUniqueId());
    }

    public RegionSelection getRegionSelection(final Player player) {
        return regionSelections.get(player.getUniqueId());
    }

    private static final int VISUALIZE_MAX_PARTICLES = 20000;
    private static final int VISUALIZE_PARTICLES_PER_BLOCK = 12;
    private static final double VISUALIZE_STDDEV_COMPENSATION = 0.25;
    private static final DustOptions VISUALIZE_DUST_INVALID = new DustOptions(Color.fromRGB(230, 60, 11), 1.0f);
    private static final DustOptions VISUALIZE_DUST_VALID = new DustOptions(Color.fromRGB(120, 220, 60), 1.0f);

    private void visualizeEdge(final World world, final BlockPos c1, final BlockPos c2, final boolean valid) {
        // Unfortunately, particle spawns are normally distributed.
        // To still have a good visualization, we need to calculate a stddev that looks
        // good.
        // Empirically, we chose a 1/2 of the radius.
        final double mx = (c1.getX() + c2.getX()) / 2.0 + 0.5;
        final double my = (c1.getY() + c2.getY()) / 2.0 + 0.5;
        final double mz = (c1.getZ() + c2.getZ()) / 2.0 + 0.5;
        double dx = Math.abs(c1.getX() - c2.getX());
        double dy = Math.abs(c1.getY() - c2.getY());
        double dz = Math.abs(c1.getZ() - c2.getZ());
        final double len = dx + dy + dz;
        final int count = Math.min(VISUALIZE_MAX_PARTICLES, (int) (VISUALIZE_PARTICLES_PER_BLOCK * len));

        // Compensate for using normal distributed particles
        dx *= VISUALIZE_STDDEV_COMPENSATION;
        dy *= VISUALIZE_STDDEV_COMPENSATION;
        dz *= VISUALIZE_STDDEV_COMPENSATION;

        // Spawn base particles
        world.spawnParticle(
            Particle.END_ROD,
            mx,
            my,
            mz,
            count,
            dx,
            dy,
            dz,
            0.0, // speed
            null, // data
            true
        ); // force

        // Spawn colored particles indicating validity
        world.spawnParticle(
            Particle.DUST,
            mx,
            my,
            mz,
            count,
            dx,
            dy,
            dz,
            0.0, // speed
            valid ? VISUALIZE_DUST_VALID : VISUALIZE_DUST_INVALID, // data
            true
        ); // force
    }

    private void visualizeSelections() {
        for (final var selectionOwner : regionSelections.keySet()) {
            final var selection = regionSelections.get(selectionOwner);
            if (selection == null) {
                continue;
            }

            // Get player for selection
            final var offlinePlayer = getServer().getOfflinePlayer(selectionOwner);
            if (!offlinePlayer.isOnline()) {
                continue;
            }
            final var player = offlinePlayer.getPlayer();

            // Both blocks are set
            if (selection.primary == null || selection.secondary == null) {
                continue;
            }

            // World match
            if (!selection.primary.getWorld().equals(selection.secondary.getWorld())) {
                continue;
            }

            // Extent can be visualized. Prepare parameters.
            final var world = selection.primary.getWorld();
            // Check if selection is valid
            final var valid = selection.isValid(player);

            final var lx = Math.min(selection.primary.getX(), selection.secondary.getX());
            final var ly = Math.min(selection.primary.getY(), selection.secondary.getY());
            final var lz = Math.min(selection.primary.getZ(), selection.secondary.getZ());
            final var hx = Math.max(selection.primary.getX(), selection.secondary.getX());
            final var hy = Math.max(selection.primary.getY(), selection.secondary.getY());
            final var hz = Math.max(selection.primary.getZ(), selection.secondary.getZ());

            // Corners
            final var A = new BlockPos(lx, ly, lz);
            final var B = new BlockPos(hx, ly, lz);
            final var C = new BlockPos(hx, hy, lz);
            final var D = new BlockPos(lx, hy, lz);
            final var E = new BlockPos(lx, ly, hz);
            final var F = new BlockPos(hx, ly, hz);
            final var G = new BlockPos(hx, hy, hz);
            final var H = new BlockPos(lx, hy, hz);

            // Visualize each edge
            visualizeEdge(world, A, B, valid);
            visualizeEdge(world, B, C, valid);
            visualizeEdge(world, C, D, valid);
            visualizeEdge(world, D, A, valid);
            visualizeEdge(world, E, F, valid);
            visualizeEdge(world, F, G, valid);
            visualizeEdge(world, G, H, valid);
            visualizeEdge(world, H, E, valid);
            visualizeEdge(world, A, E, valid);
            visualizeEdge(world, B, F, valid);
            visualizeEdge(world, C, G, valid);
            visualizeEdge(world, D, H, valid);
        }
    }

    public void addRegionGroup(final RegionGroup group) {
        storageRegionGroups.put(group.id(), group);
        markPersistentStorageDirty();
    }

    public boolean canRemoveRegionGroup(final RegionGroup group) {
        // Returns true if this region group is unused and can be removed.

        // If this region group is the fallback default group, it is permanent!
        if (storageDefaultRegionGroup.containsValue(group.id())) {
            return false;
        }

        // If any region uses this group, we can't remove it.
        return regions.values().stream().noneMatch(r -> r.regionGroupId().equals(group.id()));
    }

    public void removeRegionGroup(final RegionGroup group) {
        // Assert that this region group is unused.
        if (!canRemoveRegionGroup(group)) {
            return;
        }

        // Remove a region group from storage
        if (storageRegionGroups.remove(group.id()) == null) {
            // Was already removed
            return;
        }

        markPersistentStorageDirty();

        // Close and taint all related open menus
        getModule().getCore().menuManager.forEachOpen((player, menu) -> {
                if (
                    menu.tag() instanceof RegionGroupMenuTag &&
                    Objects.equals(((RegionGroupMenuTag) menu.tag()).regionGroupId(), group.id())
                ) {
                    menu.taint();
                    menu.close(player);
                }
            });
    }

    public RegionGroup getRegionGroup(final UUID regionGroup) {
        return storageRegionGroups.get(regionGroup);
    }

    public boolean createRegionFromSelection(final Player player, final String name) {
        final var selection = getRegionSelection(player);
        if (!selection.isValid(player)) {
            return false;
        }

        // Take currency items / withdraw economy
        final var price = selection.price();
        if (configEconomyAsCurrency) {
            if (price > 0) {
                final var transaction = economy.withdraw(player, price);
                if (!transaction.transactionSuccess()) {
                    getLog().warning(
                        "Player " +
                        player +
                        " tried to create region '" +
                        name +
                        "' (cost " +
                        price +
                        ") but the economy plugin failed to withdraw:"
                    );
                    getLog().warning("Error message: " + transaction.errorMessage);
                    return false;
                }
            }
        } else {
            final var map = new HashMap<ItemStack, Integer>();
            map.put(new ItemStack(configCurrency), (int) price);
            if (price > 0 && !takeItems(player, map)) {
                return false;
            }
        }

        final var defRegionGroup = getOrCreateDefaultRegionGroup(player);
        final var region = new Region(name, player.getUniqueId(), selection.extent(), defRegionGroup.id());
        addNewRegion(region);
        cancelRegionSelection(player);
        return true;
    }

    public String currencyString() {
        if (configEconomyAsCurrency) {
            return economy.currencyNamePlural();
        } else {
            return String.valueOf(configCurrency).toLowerCase();
        }
    }

    public void addNewRegion(final Region region) {
        region.invalidated = true;
        // Index region for fast lookup
        indexRegion(region);
    }

    public void removeRegion(final Region region) {
        // Remove region from storage
        if (regions.remove(region.id()) == null) {
            // Was already removed
            return;
        }

        // Force update storage now, as a precaution.
        updatePersistentData();

        // Close and taint all related open menus
        getModule().getCore().menuManager.forEachOpen((player, menu) -> {
                if (
                    menu.tag() instanceof RegionMenuTag &&
                    Objects.equals(((RegionMenuTag) menu.tag()).regionId(), region.id())
                ) {
                    menu.taint();
                    menu.close(player);
                }
            });

        // Remove a region from index
        indexRemoveRegion(region);

        // Remove map marker
        removeMarker(region.id());
    }

    public void updateMarker(final Region region) {
        dynmapLayer.updateMarker(region);
        blueMapLayer.updateMarker(region);
    }

    public void removeMarker(final UUID regionId) {
        dynmapLayer.removeMarker(regionId);
        blueMapLayer.removeMarker(regionId);
    }

    private void indexRegion(final Region region) {
        regions.put(region.id(), region);

        // Adds the region to the lookup map at all intersecting chunks
        final var min = region.extent().min();
        final var max = region.extent().max();

        final var worldId = min.getWorld().getUID();
        var regionsInChunk = regionsInChunkInWorld.computeIfAbsent(worldId, k -> new HashMap<>());

        final var minChunk = min.getChunk();
        final var maxChunk = max.getChunk();

        // Iterate all the chunks which intersect the region
        for (int cx = minChunk.getX(); cx <= maxChunk.getX(); ++cx) {
            for (int cz = minChunk.getZ(); cz <= maxChunk.getZ(); ++cz) {
                final var chunkKey = Chunk.getChunkKey(cx, cz);
                var possibleRegions = regionsInChunk.computeIfAbsent(chunkKey, k -> new ArrayList<>());
                possibleRegions.add(region);
            }
        }

        // Create map marker
        updateMarker(region);
    }

    private void indexRemoveRegion(final Region region) {
        // Removes the region from the lookup map at all intersecting chunks
        final var min = region.extent().min();
        final var max = region.extent().max();

        final var worldId = min.getWorld().getUID();
        final var regionsInChunk = regionsInChunkInWorld.get(worldId);
        if (regionsInChunk == null) {
            return;
        }

        final var minChunk = min.getChunk();
        final var maxChunk = max.getChunk();

        // Iterate all the chunks which intersect the region
        for (int cx = minChunk.getX(); cx <= maxChunk.getX(); ++cx) {
            for (int cz = minChunk.getZ(); cz <= maxChunk.getZ(); ++cz) {
                final var chunkKey = Chunk.getChunkKey(cx, cz);
                final var possibleRegions = regionsInChunk.get(chunkKey);
                if (possibleRegions == null) {
                    continue;
                }
                possibleRegions.remove(region);
            }
        }
    }

    public Region regionAt(final Location loc) {
        final var worldId = loc.getWorld().getUID();
        final var regionsInChunk = regionsInChunkInWorld.get(worldId);
        if (regionsInChunk == null) {
            return null;
        }

        final var chunkKey = loc.getChunk().getChunkKey();
        final var possibleRegions = regionsInChunk.get(chunkKey);
        if (possibleRegions == null) {
            return null;
        }

        for (final var region : possibleRegions) {
            if (region.extent().isInside(loc)) {
                return region;
            }
        }

        return null;
    }

    public Region regionAt(final Block block) {
        final var worldId = block.getWorld().getUID();
        final var regionsInChunk = regionsInChunkInWorld.get(worldId);
        if (regionsInChunk == null) {
            return null;
        }

        final var chunkKey = block.getChunk().getChunkKey();
        final var possibleRegions = regionsInChunk.get(chunkKey);
        if (possibleRegions == null) {
            return null;
        }

        for (final var region : possibleRegions) {
            if (region.extent().isInside(block)) {
                return region;
            }
        }

        return null;
    }

    public boolean mayAdministrate(final Player player, final RegionGroup group) {
        return (
            player.getUniqueId().equals(group.owner()) ||
            (group != null && group.getRole(player.getUniqueId()).getSetting(RoleSetting.ADMIN))
        );
    }

    public boolean mayAdministrate(final Player player, final Region region) {
        return player.getUniqueId().equals(region.owner()) || player.hasPermission(adminPermission);
    }

    public RegionGroup getOrCreateDefaultRegionGroup(final Player owner) {
        final var ownerId = owner.getUniqueId();
        final var regionGroupId = storageDefaultRegionGroup.get(ownerId);
        if (regionGroupId != null) {
            return getRegionGroup(regionGroupId);
        }

        // Create and save owner's default group
        final var regionGroup = new RegionGroup("[default] " + owner.getName(), ownerId);
        addRegionGroup(regionGroup);

        // Set group as the default
        storageDefaultRegionGroup.put(ownerId, regionGroup.id());
        markPersistentStorageDirty();

        return regionGroup;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        // Remove pending selection
        cancelRegionSelection(event.getPlayer());
    }

    @EventHandler
    public void onSaveWorld(final WorldSaveEvent event) {
        updatePersistentData(event.getWorld());
    }

    @EventHandler
    public void onLoadWorld(final WorldLoadEvent event) {
        loadPersistentData(event.getWorld());
    }

    @EventHandler
    public void onUnloadWorld(final WorldUnloadEvent event) {
        // Save data before unloading a world (not called on stop)
        updatePersistentData(event.getWorld());
    }

    public static final NamespacedKey STORAGE_REGIONS = StorageUtil.namespacedKey("vane_regions", "regions");

    public void loadPersistentData(final World world) {
        final var data = world.getPersistentDataContainer();
        final var storageRegionPrefix = STORAGE_REGIONS + ".";

        // Load all currently stored regions.
        final var pdcRegions = data
            .getKeys()
            .stream()
            .filter(key -> key.toString().startsWith(storageRegionPrefix))
            .map(key -> key.toString().substring(storageRegionPrefix.length()))
            .map(uuid -> UUID.fromString(uuid))
            .collect(Collectors.toSet());

        for (final var regionId : pdcRegions) {
            final var jsonBytes = data.get(
                NamespacedKey.fromString(storageRegionPrefix + regionId.toString()),
                PersistentDataType.BYTE_ARRAY
            );
            try {
                final var region = PersistentSerializer.fromJson(Region.class, new JSONObject(new String(jsonBytes)));
                indexRegion(region);
            } catch (IOException e) {
                getLog().log(Level.SEVERE, "error while serializing persistent data!", e);
            }
        }
        getLog().log(
            Level.INFO,
            "Loaded " + pdcRegions.size() + " regions for world " + world.getName() + "(" + world.getUID() + ")"
        );

        // Convert regions from legacy storage
        final Set<UUID> removeFromLegacyStorage = new HashSet<>();
        int converted = 0;
        for (final var region : storageRegions.values()) {
            if (!region.extent().world().equals(world.getUID())) {
                continue;
            }

            if (regions.containsKey(region.id())) {
                removeFromLegacyStorage.add(region.id());
                continue;
            }

            indexRegion(region);
            region.invalidated = true;
            converted += 1;
        }

        // Remove any region that was successfully loaded from the new storage.
        removeFromLegacyStorage.forEach(storageRegions::remove);
        if (removeFromLegacyStorage.size() > 0) {
            markPersistentStorageDirty();
        }

        // Save if we had any conversions
        if (converted > 0) {
            updatePersistentData();
        }
    }

    public void updatePersistentData() {
        for (final var world : getServer().getWorlds()) {
            updatePersistentData(world);
        }
    }

    public void updatePersistentData(final World world) {
        final var data = world.getPersistentDataContainer();
        final var storageRegionPrefix = STORAGE_REGIONS + ".";

        // Update invalidated regions
        regions
            .values()
            .stream()
            .filter(x -> x.invalidated && x.extent().world().equals(world.getUID()))
            .forEach(region -> {
                try {
                    final var json = PersistentSerializer.toJson(Region.class, region);
                    data.set(
                        NamespacedKey.fromString(storageRegionPrefix + region.id().toString()),
                        PersistentDataType.BYTE_ARRAY,
                        json.toString().getBytes()
                    );
                } catch (IOException e) {
                    getLog().log(Level.SEVERE, "error while serializing persistent data!", e);
                    return;
                }

                region.invalidated = false;
            });

        // Get all currently stored regions.
        final var storedRegions = data
            .getKeys()
            .stream()
            .filter(key -> key.toString().startsWith(storageRegionPrefix))
            .map(key -> key.toString().substring(storageRegionPrefix.length()))
            .map(uuid -> UUID.fromString(uuid))
            .collect(Collectors.toSet());

        // Remove all regions that no longer exist
        Sets.difference(storedRegions, regions.keySet()).forEach(id ->
            data.remove(NamespacedKey.fromString(storageRegionPrefix + id.toString()))
        );
    }

    @Override
    public void onModuleDisable() {
        // Save data
        updatePersistentData();
        super.onModuleDisable();
    }
}
