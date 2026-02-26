package org.oddlama.vane.portals;

import static org.oddlama.vane.util.BlockUtil.adjacentBlocks3D;
import static org.oddlama.vane.util.Conversions.msToTicks;
import static org.oddlama.vane.util.ItemUtil.nameItem;
import static org.oddlama.vane.util.Nms.itemHandle;
import static org.oddlama.vane.util.Nms.registerEntity;
import static org.oddlama.vane.util.Nms.spawn;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigMaterialMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.material.ExtendedMaterial;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.portals.entity.FloatingItem;
import org.oddlama.vane.portals.menu.PortalMenuGroup;
import org.oddlama.vane.portals.menu.PortalMenuTag;
import org.oddlama.vane.portals.portal.Orientation;
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.PortalBlockLookup;
import org.oddlama.vane.portals.portal.Style;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.StorageUtil;

@VaneModule(name = "portals", bstats = 8642, configVersion = 3, langVersion = 6, storageVersion = 2)
public class Portals extends Module<Portals> {
    // Add (de-)serializers
    static {
        PersistentSerializer.serializers.put(Orientation.class, x -> ((Orientation) x).name());
        PersistentSerializer.deserializers.put(Orientation.class, x -> Orientation.valueOf((String) x));
        PersistentSerializer.serializers.put(Portal.class, Portal::serialize);
        PersistentSerializer.deserializers.put(Portal.class, Portal::deserialize);
        PersistentSerializer.serializers.put(Portal.Visibility.class, x -> ((Portal.Visibility) x).name());
        PersistentSerializer.deserializers.put(Portal.Visibility.class, x -> Portal.Visibility.valueOf((String) x));
        PersistentSerializer.serializers.put(PortalBlock.class, PortalBlock::serialize);
        PersistentSerializer.deserializers.put(PortalBlock.class, PortalBlock::deserialize);
        PersistentSerializer.serializers.put(PortalBlock.Type.class, x -> ((PortalBlock.Type) x).name());
        PersistentSerializer.deserializers.put(PortalBlock.Type.class, x -> PortalBlock.Type.valueOf((String) x));
        PersistentSerializer.serializers.put(PortalBlockLookup.class, PortalBlockLookup::serialize);
        PersistentSerializer.deserializers.put(PortalBlockLookup.class, PortalBlockLookup::deserialize);
        PersistentSerializer.serializers.put(Style.class, Style::serialize);
        PersistentSerializer.deserializers.put(Style.class, Style::deserialize);
    }

    @ConfigMaterialSet(
        def = { Material.PISTON, Material.STICKY_PISTON },
        desc = "Materials which may not be used to decorate portals."
    )
    public Set<Material> configBlacklistedMaterials;

    // TODO better and more default styles
    @ConfigMaterialMapMapMap(
        def = {
            @ConfigMaterialMapMapMapEntry(
                key = "vane_portals:portal_style_default",
                value = {
                    @ConfigMaterialMapMapEntry(
                        key = "Active",
                        value = {
                            @ConfigMaterialMapEntry(key = "Boundary1", value = Material.OBSIDIAN),
                            @ConfigMaterialMapEntry(key = "Boundary2", value = Material.OBSIDIAN),
                            @ConfigMaterialMapEntry(key = "Boundary3", value = Material.OBSIDIAN),
                            @ConfigMaterialMapEntry(key = "Boundary4", value = Material.OBSIDIAN),
                            @ConfigMaterialMapEntry(key = "Boundary5", value = Material.OBSIDIAN),
                            @ConfigMaterialMapEntry(key = "Console", value = Material.ENCHANTING_TABLE),
                            @ConfigMaterialMapEntry(key = "Origin", value = Material.OBSIDIAN),
                            @ConfigMaterialMapEntry(key = "Portal", value = Material.END_GATEWAY),
                        }
                    ),
                    @ConfigMaterialMapMapEntry(
                        key = "Inactive",
                        value = {
                            @ConfigMaterialMapEntry(key = "Boundary1", value = Material.OBSIDIAN),
                            @ConfigMaterialMapEntry(key = "Boundary2", value = Material.OBSIDIAN),
                            @ConfigMaterialMapEntry(key = "Boundary3", value = Material.OBSIDIAN),
                            @ConfigMaterialMapEntry(key = "Boundary4", value = Material.OBSIDIAN),
                            @ConfigMaterialMapEntry(key = "Boundary5", value = Material.OBSIDIAN),
                            @ConfigMaterialMapEntry(key = "Console", value = Material.ENCHANTING_TABLE),
                            @ConfigMaterialMapEntry(key = "Origin", value = Material.OBSIDIAN),
                            @ConfigMaterialMapEntry(key = "Portal", value = Material.AIR),
                        }
                    ),
                }
            ),
            @ConfigMaterialMapMapMapEntry(
                key = "vane_portals:portal_style_aqua",
                value = {
                    @ConfigMaterialMapMapEntry(
                        key = "Active",
                        value = {
                            @ConfigMaterialMapEntry(key = "Boundary1", value = Material.DARK_PRISMARINE),
                            @ConfigMaterialMapEntry(key = "Boundary2", value = Material.WARPED_PLANKS),
                            @ConfigMaterialMapEntry(key = "Boundary3", value = Material.SEA_LANTERN),
                            @ConfigMaterialMapEntry(key = "Boundary4", value = Material.WARPED_WART_BLOCK),
                            @ConfigMaterialMapEntry(key = "Boundary5", value = Material.LIGHT_BLUE_STAINED_GLASS),
                            @ConfigMaterialMapEntry(key = "Console", value = Material.ENCHANTING_TABLE),
                            @ConfigMaterialMapEntry(key = "Origin", value = Material.DARK_PRISMARINE),
                            @ConfigMaterialMapEntry(key = "Portal", value = Material.END_GATEWAY),
                        }
                    ),
                    @ConfigMaterialMapMapEntry(
                        key = "Inactive",
                        value = {
                            @ConfigMaterialMapEntry(key = "Boundary1", value = Material.DARK_PRISMARINE),
                            @ConfigMaterialMapEntry(key = "Boundary2", value = Material.WARPED_PLANKS),
                            @ConfigMaterialMapEntry(key = "Boundary3", value = Material.PRISMARINE_BRICKS),
                            @ConfigMaterialMapEntry(key = "Boundary4", value = Material.WARPED_WART_BLOCK),
                            @ConfigMaterialMapEntry(key = "Boundary5", value = Material.LIGHT_BLUE_STAINED_GLASS),
                            @ConfigMaterialMapEntry(key = "Console", value = Material.ENCHANTING_TABLE),
                            @ConfigMaterialMapEntry(key = "Origin", value = Material.DARK_PRISMARINE),
                            @ConfigMaterialMapEntry(key = "Portal", value = Material.AIR),
                        }
                    ),
                }
            ),
        },
        desc = "Portal style definitions. Must provide a material for each portal block type and activation state. The default style may be overridden."
    )
    public Map<String, Map<String, Map<String, Material>>> configStyles;

    @ConfigLong(
        def = 10000,
        min = 1000,
        max = 110000,
        desc = "Delay in milliseconds after which two connected portals will automatically be disabled. Purple end-gateway beams do not show up until the maximum value of 110 seconds."
    )
    public long configDeactivationDelay;

    @ConfigExtendedMaterial(
        def = "vane:decoration_end_portal_orb",
        desc = "The default portal icon. Also accepts heads from the head library."
    )
    public ExtendedMaterial configDefaultIcon;

    @ConfigDouble(
        def = 0.9,
        min = 0.0,
        max = 1.0,
        desc = "Volume for the portal activation sound effect. 0 to disable."
    )
    public double configVolumeActivation;

    @ConfigDouble(
        def = 1.0,
        min = 0.0,
        max = 1.0,
        desc = "Volume for the portal deactivation sound effect. 0 to disable."
    )
    public double configVolumeDeactivation;

    @LangMessage
    public TranslatedMessage langConsoleDisplayActive;

    @LangMessage
    public TranslatedMessage langConsoleDisplayInactive;

    @LangMessage
    public TranslatedMessage langConsoleNoTarget;

    @LangMessage
    public TranslatedMessage langUnlinkRestricted;

    @LangMessage
    public TranslatedMessage langDestroyRestricted;

    @LangMessage
    public TranslatedMessage langSettingsRestricted;

    @LangMessage
    public TranslatedMessage langSelectTargetRestricted;

    // This permission allows players (usually admins) to always modify settings
    // on any portal, regardless of whether other restrictions would block access.
    public final Permission adminPermission;

    // Primary storage for all portals (portalId → portal)
    @Persistent
    private Map<UUID, Portal> storagePortals = new HashMap<>();

    private Map<UUID, Portal> portals = new HashMap<>();

    // Index for all portal blocks (worldId → chunk key → block key → portal block)
    private Map<UUID, Map<Long, Map<Long, PortalBlockLookup>>> portalBlocksInChunkInWorld = new HashMap<>();

    // All loaded styles
    public Map<NamespacedKey, Style> styles = new HashMap<>();
    // Cache possible area materials. This is fine as only predefined styles can
    // change this.
    public Set<Material> portalAreaMaterials = new HashSet<>();

    // Track console items
    private final Map<Block, FloatingItem> consoleFloatingItems = new HashMap<>();
    // Connected portals (always stores both directions!)
    private final Map<UUID, UUID> connectedPortals = new HashMap<>();
    // Unloading ticket counter per chunk
    private final Map<Long, Integer> chunkTicketCount = new HashMap<>();
    // Disable tasks for portals
    private final Map<UUID, BukkitTask> disableTasks = new HashMap<>();

    public PortalMenuGroup menus;
    public PortalConstructor constructor;
    public PortalDynmapLayer dynmapLayer;
    public PortalBlueMapLayer blueMapLayer;

    public Portals() {
        registerEntities();

        menus = new PortalMenuGroup(this);
        new PortalActivator(this);
        new PortalBlockProtector(this);
        constructor = new PortalConstructor(this);
        new PortalTeleporter(this);
        new EntityMoveProcessor(this);
        dynmapLayer = new PortalDynmapLayer(this);
        blueMapLayer = new PortalBlueMapLayer(this);

        // Register admin permission
        adminPermission = new Permission(
            "vane." + getModule().getAnnotationName() + ".admin",
            "Allows administration of any portal",
            PermissionDefault.OP
        );
        getModule().registerPermission(adminPermission);

        // TODO legacy, remove in v2.
        getPersistentStorageManager().addMigrationTo(
            2,
            "Portal visibility GROUP_INTERNAL was added. This is a no-op.",
            json -> {}
        );
    }

    @SuppressWarnings("unchecked")
    private void registerEntities() {
        getModule().getCore().unfreezeRegistries();
        registerEntity(
            NamespacedKey.minecraft("item"),
            namespace(),
            "floating_item",
            EntityType.Builder.of(FloatingItem::new, MobCategory.MISC).noSave().sized(0.0f, 0.0f)
        );
    }

    private static long blockKey(final Block block) {
        return (block.getY() << 8) | ((block.getX() & 0xF) << 4) | ((block.getZ() & 0xF));
    }

    private static Block unpackBlockKey(final Chunk chunk, long blockKey) {
        int y = (int) (blockKey >> 8);
        int x = (int) ((blockKey >> 4) & 0xF);
        int z = (int) (blockKey & 0xF);
        return chunk.getBlock(x, y, z);
    }

    @Override
    public void onConfigChange() {
        styles.clear();

        configStyles.forEach((styleKey, v1) -> {
            final var split = styleKey.split(":");
            if (split.length != 2) {
                throw new RuntimeException("Invalid style key: '" + styleKey + "' is not a valid namespaced key");
            }

            final var style = new Style(StorageUtil.namespacedKey(split[0], split[1]));
            v1.forEach((isActive, v2) -> {
                final boolean active;
                switch (isActive) {
                    case "Active":
                        active = true;
                        break;
                    case "Inactive":
                        active = false;
                        break;
                    default:
                        throw new RuntimeException("Invalid active state, must be either 'active' or 'inactive'");
                }

                v2.forEach((portalBlockType, material) -> {
                    final var type = PortalBlock.Type.valueOf(portalBlockType.toUpperCase());
                    style.setMaterial(active, type, material);
                });
            });

            // Check validity and add to map.
            style.checkValid();
            styles.put(style.key(), style);
        });

        if (!styles.containsKey(Style.defaultStyleKey())) {
            // Add default style if it wasn't overridden
            final var defaultStyle = Style.defaultStyle();
            styles.put(defaultStyle.key(), defaultStyle);
        }

        portalAreaMaterials.clear();
        // Acquire material set from styles. Will be used to speed up event checking.
        for (final var style : styles.values()) {
            portalAreaMaterials.add(style.material(true, PortalBlock.Type.PORTAL));
        }
    }

    // Lightweight callbacks to the regions module, if it is installed.
    // Lifting the callback storage into the portals module saves us
    // from having to ship regions api with this module.
    private Function2<Portal, Portal, Boolean> isInSameRegionGroupCallback = null;

    public void setIsInSameRegionGroupCallback(final Function2<Portal, Portal, Boolean> callback) {
        isInSameRegionGroupCallback = callback;
    }

    private Function2<Player, Portal, Boolean> playerCanUsePortalsInRegionGroupOfCallback = null;

    public void setPlayerCanUsePortalsInRegionGroupOfCallback(
        final Function2<Player, Portal, Boolean> callback
    ) {
        playerCanUsePortalsInRegionGroupOfCallback = callback;
    }

    public boolean isInSameRegionGroup(final Portal a, final Portal b) {
        if (isInSameRegionGroupCallback == null) {
            return true;
        }
        return isInSameRegionGroupCallback.apply(a, b);
    }

    public boolean playerCanUsePortalsInRegionGroupOf(final Player player, final Portal portal) {
        if (playerCanUsePortalsInRegionGroupOfCallback == null) {
            return true;
        }
        return playerCanUsePortalsInRegionGroupOfCallback.apply(player, portal);
    }

    public boolean isRegionsInstalled() {
        return isInSameRegionGroupCallback != null;
    }

    public Style style(final NamespacedKey key) {
        final var s = styles.get(key);
        if (s == null) {
            getLogger().warning("Encountered invalid style " + key + ", falling back to default style.");
            return styles.get(Style.defaultStyleKey());
        } else {
            return s;
        }
    }

    public void removePortal(final Portal portal) {
        // Deactivate portal if needed
        final var connected = connectedPortal(portal);
        if (connected != null) {
            disconnectPortals(portal, connected);
        }

        // Remove portal from storage
        if (portals.remove(portal.id()) == null) {
            // Was already removed
            return;
        }

        // Remove portal blocks
        portal.blocks().forEach(this::removePortalBlock);

        // Replace references to the portal everywhere
        // and update all changed portal consoles.
        for (final var other : portals.values()) {
            if (Objects.equals(other.targetId(), portal.id())) {
                other.targetId(null);
                other
                    .blocks()
                    .stream()
                    .filter(pb -> pb.type() == PortalBlock.Type.CONSOLE)
                    .filter(pb -> consoleFloatingItems.containsKey(pb.block()))
                    .forEach(pb -> updateConsoleItem(other, pb.block()));
            }
        }

        // Force update storage now, as a precaution.
        updatePersistentData();

        // Close and taint all related open menus
        getModule()
            .getCore().menuManager.forEachOpen((player, menu) -> {
                if (
                    menu.tag() instanceof PortalMenuTag &&
                    Objects.equals(((PortalMenuTag) menu.tag()).portalId(), portal.id())
                ) {
                    menu.taint();
                    menu.close(player);
                }
            });

        // Remove map marker
        removeMarker(portal.id());

        // Play sound
        portal
            .spawn()
            .getWorld()
            .playSound(portal.spawn(), Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    public void addNewPortal(final Portal portal) {
        portal.invalidated = true;

        // Index the new portal
        indexPortal(portal);

        // Play sound
        portal
            .spawn()
            .getWorld()
            .playSound(portal.spawn(), Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.BLOCKS, 1.0f, 2.0f);
    }

    public void indexPortal(final Portal portal) {
        portals.put(portal.id(), portal);
        portal.blocks().forEach(b -> indexPortalBlock(portal, b));

        // Create map marker
        updateMarker(portal);
    }

    public Collection<Portal> allAvailablePortals() {
        return portals.values().stream().filter(p -> p.spawn().isWorldLoaded()).collect(Collectors.toList());
    }

    public void removePortalBlock(final PortalBlock portalBlock) {
        // Restore original block
        switch (portalBlock.type()) {
            case ORIGIN:
                portalBlock.block().setType(constructor.configMaterialOrigin);
                break;
            case CONSOLE:
                portalBlock.block().setType(constructor.configMaterialConsole);
                break;
            case BOUNDARY1:
                portalBlock.block().setType(constructor.configMaterialBoundary1);
                break;
            case BOUNDARY2:
                portalBlock.block().setType(constructor.configMaterialBoundary2);
                break;
            case BOUNDARY3:
                portalBlock.block().setType(constructor.configMaterialBoundary3);
                break;
            case BOUNDARY4:
                portalBlock.block().setType(constructor.configMaterialBoundary4);
                break;
            case BOUNDARY5:
                portalBlock.block().setType(constructor.configMaterialBoundary5);
                break;
            case PORTAL:
                portalBlock.block().setType(constructor.configMaterialPortalArea);
                break;
        }

        // Remove console item if a block is a console
        if (portalBlock.type() == PortalBlock.Type.CONSOLE) {
            removeConsoleItem(portalBlock.block());
        }

        // Remove from acceleration structure
        final var block = portalBlock.block();
        final var portalBlocksInChunk = portalBlocksInChunkInWorld.get(block.getWorld().getUID());
        if (portalBlocksInChunk == null) {
            return;
        }

        final var chunkKey = block.getChunk().getChunkKey();
        final var blockToPortalBlock = portalBlocksInChunk.get(chunkKey);
        if (blockToPortalBlock == null) {
            return;
        }

        blockToPortalBlock.remove(blockKey(block));

        // Spawn effect if not portal area
        if (portalBlock.type() != PortalBlock.Type.PORTAL) {
            portalBlock
                .block()
                .getWorld()
                .spawnParticle(
                    Particle.ENCHANT,
                    portalBlock.block().getLocation().add(0.5, 0.5, 0.5),
                    50,
                    0.0,
                    0.0,
                    0.0,
                    1.0
                );
        }
    }

    public void removePortalBlock(final Portal portal, final PortalBlock portalBlock) {
        // Remove from portal
        portal.blocks().remove(portalBlock);

        // Remove from acceleration structure
        removePortalBlock(portalBlock);
    }

    public void addNewPortalBlock(final Portal portal, final PortalBlock portalBlock) {
        // Add to portal
        portal.blocks().add(portalBlock);
        portal.invalidated = true;

        indexPortalBlock(portal, portalBlock);

        // Spawn effect if not portal area
        if (portalBlock.type() != PortalBlock.Type.PORTAL) {
            portalBlock
                .block()
                .getWorld()
                .spawnParticle(
                    Particle.PORTAL,
                    portalBlock.block().getLocation().add(0.5, 0.5, 0.5),
                    50,
                    0.0,
                    0.0,
                    0.0,
                    1.0
                );
        }
    }

    public void indexPortalBlock(final Portal portal, final PortalBlock portalBlock) {
        // Add to acceleration structure
        final var block = portalBlock.block();
        final var worldId = block.getWorld().getUID();
        var portalBlocksInChunk = portalBlocksInChunkInWorld.computeIfAbsent(worldId, k -> new HashMap<>());

        final var chunkKey = block.getChunk().getChunkKey();
        var blockToPortalBlock = portalBlocksInChunk.computeIfAbsent(chunkKey, k -> new HashMap<>());

        blockToPortalBlock.put(blockKey(block), portalBlock.lookup(portal.id()));
    }

    public PortalBlockLookup portalBlockFor(final Block block) {
        final var portalBlocksInChunk = portalBlocksInChunkInWorld.get(block.getWorld().getUID());
        if (portalBlocksInChunk == null) {
            return null;
        }

        final var chunkKey = block.getChunk().getChunkKey();
        final var blockToPortalBlock = portalBlocksInChunk.get(chunkKey);
        if (blockToPortalBlock == null) {
            return null;
        }

        return blockToPortalBlock.get(blockKey(block));
    }

    public Portal portalFor(@Nullable final UUID uuid) {
        final var portal = portals.get(uuid);
        if (portal == null || !portal.spawn().isWorldLoaded()) {
            return null;
        }
        return portal;
    }

    public Portal portalFor(@NotNull final PortalBlockLookup block) {
        return portalFor(block.portalId());
    }

    public Portal portalFor(final Block block) {
        final var portalBlock = portalBlockFor(block);
        if (portalBlock == null) {
            return null;
        }

        return portalFor(portalBlock);
    }

    public boolean isPortalBlock(final Block block) {
        final var portalBlocksInChunk = portalBlocksInChunkInWorld.get(block.getWorld().getUID());
        if (portalBlocksInChunk == null) {
            return false;
        }

        final var chunkKey = block.getChunk().getChunkKey();
        final var blockToPortalBlock = portalBlocksInChunk.get(chunkKey);
        if (blockToPortalBlock == null) {
            return false;
        }

        return blockToPortalBlock.containsKey(blockKey(block));
    }

    public Portal controlledPortal(final Block block) {
        final var rootPortal = portalFor(block);
        if (rootPortal != null) {
            return rootPortal;
        }

        // Find adjacent console blocks in full 3x3x3 cube, which will make this block a
        // controlling block
        for (final var adj : adjacentBlocks3D(block)) {
            final var portalBlock = portalBlockFor(adj);
            if (portalBlock != null && portalBlock.type() == PortalBlock.Type.CONSOLE) {
                return portalFor(portalBlock);
            }
        }

        return null;
    }

    public Set<Chunk> chunksFor(final Portal portal) {
        if (portal == null) {
            return new HashSet<Chunk>();
        }

        final var set = new HashSet<Chunk>();
        for (final var pb : portal.blocks()) {
            set.add(pb.block().getChunk());
        }
        return set;
    }

    public void loadPortalChunks(final Portal portal) {
        // Load chunks and adds a ticket, so they get loaded and are kept loaded
        for (final var chunk : chunksFor(portal)) {
            final var chunkKey = chunk.getChunkKey();
            final var ticketCounter = chunkTicketCount.get(chunkKey);
            if (ticketCounter == null) {
                chunk.addPluginChunkTicket(this);
                chunkTicketCount.put(chunkKey, 1);
            } else {
                chunkTicketCount.put(chunkKey, ticketCounter + 1);
            }
        }
    }

    public void allowUnloadPortalChunks(final Portal portal) {
        // Removes the ticket so chunks can be unloaded again
        for (final var chunk : chunksFor(portal)) {
            final var chunkKey = chunk.getChunkKey();
            final var ticketCounter = chunkTicketCount.get(chunkKey);

            if (ticketCounter > 1) {
                chunkTicketCount.put(chunkKey, ticketCounter - 1);
            } else if (ticketCounter == 1) {
                chunk.removePluginChunkTicket(this);
                chunkTicketCount.remove(chunkKey);
            }
        }
    }

    public void connectPortals(final Portal src, final Portal dst) {
        // Load chunks
        loadPortalChunks(src);
        loadPortalChunks(dst);

        // Add to map
        connectedPortals.put(src.id(), dst.id());
        connectedPortals.put(dst.id(), src.id());

        // Activate both
        src.onConnect(this, dst);
        dst.onConnect(this, src);

        // Schedule automatic disable
        startDisableTask(src, dst);
    }

    public void disconnectPortals(final Portal src) {
        disconnectPortals(src, portalFor(connectedPortals.get(src.id())));
    }

    public void disconnectPortals(final Portal src, final Portal dst) {
        if (src == null || dst == null) {
            return;
        }

        // Allow unloading chunks again
        allowUnloadPortalChunks(src);
        allowUnloadPortalChunks(dst);

        // Remove from a map
        connectedPortals.remove(src.id());
        connectedPortals.remove(dst.id());

        // Deactivate both
        src.onDisconnect(this, dst);
        dst.onDisconnect(this, src);

        // Reset target id's if the target portal was transient and
        // the target isn't locked.
        if (dst.visibility().isTransientTarget() && !src.targetLocked()) {
            src.targetId(null);
            src.updateBlocks(this);
        }

        // Remove an automatic disable task if existing
        stopDisableTask(src, dst);
    }

    private void startDisableTask(final Portal portal, final Portal target) {
        stopDisableTask(portal, target);
        final var task = scheduleTask(
            new PortalDisableRunnable(portal, target),
            msToTicks(configDeactivationDelay)
        );
        disableTasks.put(portal.id(), task);
        disableTasks.put(target.id(), task);
    }

    private void stopDisableTask(final Portal portal, final Portal target) {
        final var task1 = disableTasks.remove(portal.id());
        final var task2 = disableTasks.remove(target.id());
        if (task1 != null) {
            task1.cancel();
        }
        if (task2 != null && task2 != task1) {
            task2.cancel();
        }
    }

    @Override
    public void onModuleDisable() {
        // Disable all portals now
        for (final var id : new ArrayList<>(connectedPortals.keySet())) {
            disconnectPortals(portalFor(id));
        }

        // Remove all console items, and all chunk tickets
        chunkTicketCount.clear();
        for (final var world : getServer().getWorlds()) {
            for (final var chunk : world.getLoadedChunks()) {
                // Remove console item
                forEachConsoleBlockInChunk(chunk, (block, console) -> removeConsoleItem(block));
                // Allow chunk unloading
                chunk.removePluginChunkTicket(this);
            }
        }

        // Save data
        updatePersistentData();
        super.onModuleDisable();
    }

    public boolean isActivated(final Portal portal) {
        return connectedPortals.containsKey(portal.id());
    }

    public Portal connectedPortal(final Portal portal) {
        final var connectedId = connectedPortals.get(portal.id());
        if (connectedId == null) {
            return null;
        }
        return portalFor(connectedId);
    }

    public ItemStack iconFor(final Portal portal) {
        final var item = portal.icon();
        if (item == null) {
            return configDefaultIcon.item();
        } else {
            return item;
        }
    }

    private ItemStack makeConsoleItem(final Portal portal, boolean active) {
        final Portal target;
        if (active) {
            target = connectedPortal(portal);
        } else {
            target = portal.target(this);
        }

        // Try to use target portal's block
        ItemStack item = null;
        if (target != null) {
            item = target.icon();
        }

        // Fallback item
        if (item == null) {
            item = configDefaultIcon.item();
        }

        final var targetName = target == null ? langConsoleNoTarget.str() : target.name();
        final Component displayName;
        if (active) {
            displayName = langConsoleDisplayActive.format("§5" + targetName);
        } else {
            displayName = langConsoleDisplayInactive.format("§7" + targetName);
        }

        return ItemUtil.nameItem(item, displayName);
    }

    public void updatePortalIcon(final Portal portal) {
        // Update map marker, as name could have changed
        updateMarker(portal);

        for (final var activeConsole : consoleFloatingItems.keySet()) {
            final var portalBlock = portalBlockFor(activeConsole);
            final var other = portalFor(portalBlock);
            if (Objects.equals(other.targetId(), portal.id())) {
                updateConsoleItem(other, activeConsole);
            }
        }
    }

    public void updatePortalVisibility(final Portal portal) {
        // Replace references to the portal everywhere if visibility
        // has changed.
        switch (portal.visibility()) {
            case PRIVATE:
            case GROUP:
                // Not visible from outside, these are transient.
                for (final var other : portals.values()) {
                    if (Objects.equals(other.targetId(), portal.id())) {
                        other.targetId(null);
                    }
                }
                break;
            case GROUP_INTERNAL:
                // Remove from portals outside the group
                for (final var other : portals.values()) {
                    if (Objects.equals(other.targetId(), portal.id()) && !isInSameRegionGroup(other, portal)) {
                        other.targetId(null);
                    }
                }
                break;
            default: // Nothing to do
                break;
        }

        // Update map marker
        updateMarker(portal);
    }

    public void updateConsoleItem(final Portal portal, final Block block) {
        var consoleItem = consoleFloatingItems.get(block);
        final boolean isNew;
        if (consoleItem == null) {
            consoleItem = new FloatingItem(
                block.getWorld(),
                block.getX() + 0.5,
                block.getY() + 1.2,
                block.getZ() + 0.5
            );
            isNew = true;
        } else {
            isNew = false;
        }

        final var active = isActivated(portal);
        consoleItem.setItem(itemHandle(makeConsoleItem(portal, active)));

        if (isNew) {
            consoleFloatingItems.put(block, consoleItem);
            spawn(block.getWorld(), consoleItem);
        }
    }

    public void removeConsoleItem(final Block block) {
        final var consoleItem = consoleFloatingItems.remove(block);
        if (consoleItem != null) {
            consoleItem.discard();
        }
    }

    private void forEachConsoleBlockInChunk(
        final Chunk chunk,
        final Consumer2<Block, PortalBlockLookup> consumer
    ) {
        final var portalBlocksInChunk = portalBlocksInChunkInWorld.get(chunk.getWorld().getUID());
        if (portalBlocksInChunk == null) {
            return;
        }

        final var chunkKey = chunk.getChunkKey();
        final var blockToPortalBlock = portalBlocksInChunk.get(chunkKey);
        if (blockToPortalBlock == null) {
            return;
        }

        blockToPortalBlock.forEach((k, v) -> {
            if (v.type() == PortalBlock.Type.CONSOLE) {
                consumer.apply(unpackBlockKey(chunk, k), v);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonitorChunkUnload(final ChunkUnloadEvent event) {
        final var chunk = event.getChunk();

        // Disable all consoles in this chunk
        forEachConsoleBlockInChunk(chunk, (block, console) -> removeConsoleItem(block));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonitorChunkLoad(final ChunkLoadEvent event) {
        final var chunk = event.getChunk();

        // Enable all consoles in this chunk
        forEachConsoleBlockInChunk(chunk, (block, console) -> {
            final var portal = portalFor(console.portalId());
            updateConsoleItem(portal, block);
        });
    }

    public void updateMarker(final Portal portal) {
        dynmapLayer.updateMarker(portal);
        blueMapLayer.updateMarker(portal);
    }

    public void removeMarker(final UUID portalId) {
        dynmapLayer.removeMarker(portalId);
        blueMapLayer.removeMarker(portalId);
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

    public void updatePersistentData() {
        for (final var world : getServer().getWorlds()) {
            updatePersistentData(world);
        }
    }

    public static final NamespacedKey STORAGE_PORTALS = StorageUtil.namespacedKey("vane_portals", "portals");

    public void loadPersistentData(final World world) {
        final var data = world.getPersistentDataContainer();
        final var storagePortalPrefix = STORAGE_PORTALS + ".";

        // Load all currently stored portals.
        final var pdcPortals = data
            .getKeys()
            .stream()
            .filter(key -> key.toString().startsWith(storagePortalPrefix))
            .map(key -> key.toString().substring(storagePortalPrefix.length()))
            .map(uuid -> UUID.fromString(uuid))
            .collect(Collectors.toSet());

        for (final var portalId : pdcPortals) {
            final var jsonBytes = data.get(
                NamespacedKey.fromString(storagePortalPrefix + portalId.toString()),
                PersistentDataType.BYTE_ARRAY
            );
            try {
                final var portal = PersistentSerializer.fromJson(Portal.class, new JSONObject(new String(jsonBytes)));
                indexPortal(portal);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "error while serializing persistent data!", e);
            }
        }
        getLogger().log(
            Level.INFO,
            "Loaded " + pdcPortals.size() + " portals for world " + world.getName() + "(" + world.getUID() + ")"
        );

        // Convert portals from legacy storage
        final Set<UUID> removeFromLegacyStorage = new HashSet<>();
        int converted = 0;
        for (final var portal : storagePortals.values()) {
            if (!portal.spawnWorld().equals(world.getUID())) {
                continue;
            }

            if (portals.containsKey(portal.id())) {
                removeFromLegacyStorage.add(portal.id());
                continue;
            }

            indexPortal(portal);
            portal.invalidated = true;
            converted += 1;
        }

        // Remove any portal that was successfully loaded from the new storage.
        removeFromLegacyStorage.forEach(storagePortals::remove);
        if (removeFromLegacyStorage.size() > 0) {
            markPersistentStorageDirty();
        }

        // Update all consoles in the loaded world. These
        // might be missed by chunk load event as it runs asynchronous
        // to this function, and it can't be synchronized without annoying the server.
        for (final var chunk : world.getLoadedChunks()) {
            forEachConsoleBlockInChunk(chunk, (block, console) -> {
                final var portal = portalFor(console.portalId());
                updateConsoleItem(portal, block);
            });
        }

        // Save if we had any conversions
        if (converted > 0) {
            updatePersistentData();
        }
    }

    public void updatePersistentData(final World world) {
        final var data = world.getPersistentDataContainer();
        final var storagePortalPrefix = STORAGE_PORTALS + ".";

        // Update invalidated portals
        portals
            .values()
            .stream()
            .filter(x -> x.invalidated && x.spawnWorld().equals(world.getUID()))
            .forEach(portal -> {
                try {
                    final var json = PersistentSerializer.toJson(Portal.class, portal);
                    data.set(
                        NamespacedKey.fromString(storagePortalPrefix + portal.id().toString()),
                        PersistentDataType.BYTE_ARRAY,
                        json.toString().getBytes()
                    );
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "error while serializing persistent data!", e);
                    return;
                }

                portal.invalidated = false;
            });

        // Get all currently stored portals.
        final var storedPortals = data
            .getKeys()
            .stream()
            .filter(key -> key.toString().startsWith(storagePortalPrefix))
            .map(key -> key.toString().substring(storagePortalPrefix.length()))
            .map(uuid -> UUID.fromString(uuid))
            .collect(Collectors.toSet());

        // Remove all portals that no longer exist
        Sets.difference(storedPortals, portals.keySet()).forEach(id ->
            data.remove(NamespacedKey.fromString(storagePortalPrefix + id.toString()))
        );
    }

    private class PortalDisableRunnable implements Runnable {

        private Portal src;
        private Portal dst;

        public PortalDisableRunnable(final Portal src, final Portal dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public void run() {
            Portals.this.disconnectPortals(src, dst);
        }
    }
}
