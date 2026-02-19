package org.oddlama.vane.portals.portal;

import static org.oddlama.vane.core.persistent.PersistentSerializer.fromJson;
import static org.oddlama.vane.core.persistent.PersistentSerializer.toJson;
import static org.oddlama.vane.util.BlockUtil.adjacentBlocks3D;
import static org.oddlama.vane.util.BlockUtil.updateLever;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.EndGateway;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.portals.Portals;
import org.oddlama.vane.portals.event.PortalActivateEvent;
import org.oddlama.vane.portals.event.PortalDeactivateEvent;
import org.oddlama.vane.portals.event.PortalOpenConsoleEvent;
import org.oddlama.vane.util.BlockUtil;
import org.oddlama.vane.util.LazyLocation;

public class Portal {

    public static Object serialize(@NotNull final Object o) throws IOException {
        final var portal = (Portal) o;
        final var json = new JSONObject();
        json.put("id", PersistentSerializer.toJson(UUID.class, portal.id));
        json.put("owner", PersistentSerializer.toJson(UUID.class, portal.owner));
        json.put("orientation", PersistentSerializer.toJson(Orientation.class, portal.orientation));
        json.put("spawn", PersistentSerializer.toJson(LazyLocation.class, portal.spawn));
        try {
            json.put("blocks", PersistentSerializer.toJson(Portal.class.getDeclaredField("blocks"), portal.blocks));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }

        json.put("name", PersistentSerializer.toJson(String.class, portal.name));
        json.put("style", PersistentSerializer.toJson(NamespacedKey.class, portal.style));
        json.put("styleOverride", PersistentSerializer.toJson(Style.class, portal.styleOverride));
        json.put("icon", PersistentSerializer.toJson(ItemStack.class, portal.icon));
        json.put("visibility", PersistentSerializer.toJson(Visibility.class, portal.visibility));

        json.put("exitOrientationLocked", PersistentSerializer.toJson(boolean.class, portal.exitOrientationLocked));
        json.put("targetId", PersistentSerializer.toJson(UUID.class, portal.targetId));
        json.put("targetLocked", PersistentSerializer.toJson(boolean.class, portal.targetLocked));
        return json;
    }

    @SuppressWarnings("unchecked")
    public static Portal deserialize(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var portal = new Portal();
        portal.id = PersistentSerializer.fromJson(UUID.class, json.get("id"));
        portal.owner = PersistentSerializer.fromJson(UUID.class, json.get("owner"));
        portal.orientation = PersistentSerializer.fromJson(Orientation.class, json.get("orientation"));
        portal.spawn = PersistentSerializer.fromJson(LazyLocation.class, json.get("spawn"));
        try {
            portal.blocks = (List<PortalBlock>) PersistentSerializer.fromJson(Portal.class.getDeclaredField("blocks"), json.get("blocks"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }

        portal.name = PersistentSerializer.fromJson(String.class, json.get("name"));
        portal.style = PersistentSerializer.fromJson(NamespacedKey.class, json.get("style"));
        portal.styleOverride = PersistentSerializer.fromJson(Style.class, json.get("styleOverride"));
        if (portal.styleOverride != null) {
            try {
                portal.styleOverride.checkValid();
            } catch (RuntimeException e) {
                portal.styleOverride = null;
            }
        }
        portal.icon = PersistentSerializer.fromJson(ItemStack.class, json.get("icon"));
        portal.visibility = PersistentSerializer.fromJson(Visibility.class, json.get("visibility"));

        portal.exitOrientationLocked = PersistentSerializer.fromJson(boolean.class, json.optString("exitOrientationLocked", "false"));
        portal.targetId = PersistentSerializer.fromJson(UUID.class, json.get("targetId"));
        portal.targetLocked = PersistentSerializer.fromJson(boolean.class, json.get("targetLocked"));
        return portal;
    }

    private UUID id;
    private UUID owner;
    private Orientation orientation;
    private LazyLocation spawn;
    private List<PortalBlock> blocks = new ArrayList<>();

    private String name = "Portal";
    private NamespacedKey style = Style.defaultStyleKey();
    private Style styleOverride = null;
    private ItemStack icon = null;
    private Visibility visibility = Visibility.PRIVATE;

    private boolean exitOrientationLocked = false;
    private UUID targetId = null;
    private boolean targetLocked = false;

    // Whether the portal should be saved on the next occasion.
    // Not a saved field.
    public boolean invalidated = true;

    private Portal() {}

    public Portal(final UUID owner, final Orientation orientation, final Location spawn) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.orientation = orientation;
        this.spawn = new LazyLocation(spawn.clone());
    }

    public UUID id() {
        return id;
    }

    public UUID owner() {
        return owner;
    }

    public Orientation orientation() {
        return orientation;
    }

    public UUID spawnWorld() {
        return spawn.worldId();
    }

    public Location spawn() {
        return spawn.location().clone();
    }

    public List<PortalBlock> blocks() {
        return blocks;
    }

    public String name() {
        return name;
    }

    public void name(final String name) {
        this.name = name;
        this.invalidated = true;
    }

    public NamespacedKey style() {
        return styleOverride == null ? style : null;
    }

    public void style(final Style style) {
        if (style.key() == null) {
            this.styleOverride = style;
        } else {
            this.style = style.key();
        }
        this.invalidated = true;
    }

    public ItemStack icon() {
        return icon == null ? null : icon.clone();
    }

    public void icon(final ItemStack icon) {
        this.icon = icon;
        this.invalidated = true;
    }

    public Visibility visibility() {
        return visibility;
    }

    public void visibility(final Visibility visibility) {
        this.visibility = visibility;
        this.invalidated = true;
    }

    public boolean exitOrientationLocked() {
        return exitOrientationLocked;
    }

    public void exitOrientationLocked(boolean exitOrientationLocked) {
        this.exitOrientationLocked = exitOrientationLocked;
        this.invalidated = true;
    }

    public UUID targetId() {
        return targetId;
    }

    public void targetId(final UUID targetId) {
        this.targetId = targetId;
        this.invalidated = true;
    }

    public boolean targetLocked() {
        return targetLocked;
    }

    public void targetLocked(boolean targetLocked) {
        this.targetLocked = targetLocked;
        this.invalidated = true;
    }

    public PortalBlock portalBlockFor(final Block block) {
        for (final var pb : blocks()) {
            if (pb.block().equals(block)) {
                return pb;
            }
        }
        return null;
    }

    public @Nullable Portal target(final Portals portals) {
        return portals.portalFor(targetId());
    }

    private Set<Block> controllingBlocks() {
        final var controllingBlocks = new HashSet<Block>();
        for (final var pb : blocks()) {
            switch (pb.type()) {
                default:
                    break;
                case ORIGIN:
                case BOUNDARY1:
                case BOUNDARY2:
                case BOUNDARY3:
                case BOUNDARY4:
                case BOUNDARY5:
                    controllingBlocks.add(pb.block());
                    break;
                case CONSOLE:
                    controllingBlocks.add(pb.block());
                    controllingBlocks.addAll(Arrays.asList(adjacentBlocks3D(pb.block())));
                    break;
            }
        }
        return controllingBlocks;
    }

    private void setControllingLevers(boolean activated) {
        final var controllingBlocks = controllingBlocks();
        final var levers = new HashSet<Block>();
        for (final var b : controllingBlocks()) {
            for (final var f : BlockUtil.BLOCK_FACES) {
                final var l = b.getRelative(f);
                if (l.getType() != Material.LEVER) {
                    continue;
                }

                final var lever = (Switch) l.getBlockData();
                final BlockFace attachedFace;
                switch (lever.getAttachedFace()) {
                    default:
                    case WALL:
                        attachedFace = lever.getFacing().getOppositeFace();
                        break;
                    case CEILING:
                        attachedFace = BlockFace.UP;
                        break;
                    case FLOOR:
                        attachedFace = BlockFace.DOWN;
                        break;
                }

                // Only when attached to a controlling block
                if (!controllingBlocks.contains(l.getRelative(attachedFace))) {
                    continue;
                }

                levers.add(l);
            }
        }

        for (final var l : levers) {
            final var lever = (Switch) l.getBlockData();
            lever.setPowered(activated);
            l.setBlockData(lever);
            updateLever(l, lever.getFacing());
        }
    }

    public boolean activate(final Portals portals, @Nullable final Player player) {
        if (portals.isActivated(this)) {
            return false;
        }

        final var target = target(portals);
        if (target == null) {
            return false;
        }

        // Call event
        final var event = new PortalActivateEvent(player, this, target);
        portals.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        portals.connectPortals(this, target);
        return true;
    }

    public boolean deactivate(final Portals portals, @Nullable final Player player) {
        if (!portals.isActivated(this)) {
            return false;
        }

        // Call event
        final var event = new PortalDeactivateEvent(player, this);
        portals.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        portals.disconnectPortals(this);
        return true;
    }

    public void onConnect(final Portals portals, final Portal target) {
        // Update blocks
        updateBlocks(portals);

        // Activate all controlling levers
        setControllingLevers(true);

        float soundVolume = (float) portals.configVolumeActivation;
        if (soundVolume > 0.0f) {
            // Play sound
            spawn()
                .getWorld()
                .playSound(spawn(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, soundVolume, 0.8f);
        }
    }

    public void onDisconnect(final Portals portals, final Portal target) {
        // Update blocks
        updateBlocks(portals);

        // Deactivate all controlling levers
        setControllingLevers(false);

        float soundVolume = (float) portals.configVolumeDeactivation;
        if (soundVolume > 0.0f) {
            // Play sound
            spawn()
                .getWorld()
                .playSound(spawn(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, soundVolume, 0.5f);
        }
    }

    public void updateBlocks(final Portals portals) {
        final Style curStyle;
        if (styleOverride == null) {
            curStyle = portals.style(style);
        } else {
            curStyle = styleOverride;
        }

        final var active = portals.isActivated(this);
        for (final var portalBlock : blocks) {
            final var type = curStyle.material(active, portalBlock.type());
            portalBlock.block().setType(type);
            if (type == Material.END_GATEWAY) {
                // Disable beam
                final var endGateway = (EndGateway) portalBlock.block().getState(false);
                endGateway.setAge(200l);
                endGateway.update(true, false);

                // If there's no exit location, then the game will generate a natural gateway when
                // the portal is used.
                // Setting any location will do, since the teleporting is canceled via their events
                // anyway.
                if (spawn.location().getWorld().getEnvironment() == World.Environment.THE_END) {
                    endGateway.setExitLocation(spawn.location());
                    endGateway.setExactTeleport(true);
                }
            }
            if (portalBlock.type() == PortalBlock.Type.CONSOLE) {
                portals.updateConsoleItem(this, portalBlock.block());
            }
        }
    }

    public boolean openConsole(final Portals portals, final Player player, final Block console) {
        // Call event
        final var event = new PortalOpenConsoleEvent(player, console, this);
        portals.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() && !player.hasPermission(portals.adminPermission)) {
            return false;
        }

        portals.menus.consoleMenu.create(this, player, console).open(player);
        return true;
    }

    public Style copyStyle(final Portals portals, final NamespacedKey newKey) {
        if (styleOverride == null) {
            return portals.style(style).copy(newKey);
        }
        return styleOverride.copy(newKey);
    }

    @Override
    public String toString() {
        return "Portal{id = " + id + ", name = " + name + "}";
    }

    public static enum Visibility {
        PUBLIC,
        GROUP,
        GROUP_INTERNAL,
        PRIVATE;

        public Visibility prev() {
            final int prev;
            if (ordinal() == 0) {
                prev = values().length - 1;
            } else {
                prev = ordinal() - 1;
            }
            return values()[prev];
        }

        public Visibility next() {
            final var next = (ordinal() + 1) % values().length;
            return values()[next];
        }

        public boolean isTransientTarget() {
            return this == GROUP || this == PRIVATE;
        }

        public boolean requiresRegions() {
            return this == GROUP || this == GROUP_INTERNAL;
        }
    }

    public static class TargetSelectionComparator implements Comparator<Portal> {

        private World world;
        private Vector from;

        public TargetSelectionComparator(final Player player) {
            this.world = player.getLocation().getWorld();
            this.from = player.getLocation().toVector().setY(0.0);
        }

        @Override
        public int compare(final Portal a, final Portal b) {
            boolean aSameWorld = world.equals(a.spawn().getWorld());
            boolean bSameWorld = world.equals(b.spawn().getWorld());

            if (aSameWorld) {
                if (bSameWorld) {
                    final var aDist = from.distanceSquared(a.spawn().toVector().setY(0.0));
                    final var bDist = from.distanceSquared(b.spawn().toVector().setY(0.0));
                    return Double.compare(aDist, bDist);
                } else {
                    return -1;
                }
            } else {
                if (bSameWorld) {
                    return 1;
                } else {
                    return a.name().compareToIgnoreCase(b.name());
                }
            }
        }
    }
}
