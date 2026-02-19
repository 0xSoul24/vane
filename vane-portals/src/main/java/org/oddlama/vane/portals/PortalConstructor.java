package org.oddlama.vane.portals;

import static org.oddlama.vane.util.PlayerUtil.swingArm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.portals.event.PortalConstructEvent;
import org.oddlama.vane.portals.event.PortalLinkConsoleEvent;
import org.oddlama.vane.portals.portal.Orientation;
import org.oddlama.vane.portals.portal.Plane;
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.PortalBoundary;

public class PortalConstructor extends Listener<Portals> {

    @ConfigMaterial(def = Material.ENCHANTING_TABLE, desc = "The block used to build portal consoles.")
    public Material configMaterialConsole;

    @ConfigMaterial(def = Material.OBSIDIAN, desc = "The block used to build the portal boundary. Variation 1.")
    public Material configMaterialBoundary1;

    @ConfigMaterial(def = Material.CRYING_OBSIDIAN, desc = "The block used to build the portal boundary. Variation 2.")
    public Material configMaterialBoundary2;

    @ConfigMaterial(def = Material.GOLD_BLOCK, desc = "The block used to build the portal boundary. Variation 3.")
    public Material configMaterialBoundary3;

    @ConfigMaterial(
        def = Material.GILDED_BLACKSTONE,
        desc = "The block used to build the portal boundary. Variation 4."
    )
    public Material configMaterialBoundary4;

    @ConfigMaterial(def = Material.EMERALD_BLOCK, desc = "The block used to build the portal boundary. Variation 5.")
    public Material configMaterialBoundary5;

    @ConfigMaterial(def = Material.NETHERITE_BLOCK, desc = "The block used to build the portal origin.")
    public Material configMaterialOrigin;

    @ConfigMaterial(def = Material.AIR, desc = "The block used to build the portal area.")
    public Material configMaterialPortalArea;

    @ConfigInt(def = 12, min = 1, desc = "Maximum horizontal distance between a console block and the portal.")
    public int configConsoleMaxDistanceXz;

    @ConfigInt(def = 12, min = 1, desc = "Maximum vertical distance between a console block and the portal.")
    public int configConsoleMaxDistanceY;

    @ConfigInt(
        def = 1024,
        min = 256,
        desc = "Maximum steps for the floodfill algorithm. This should only be increased if you want really big portals. It's recommended to keep this as low as possible."
    )
    public int configAreaFloodfillMaxSteps = 1024;

    @ConfigInt(def = 24, min = 8, desc = "Maximum portal area width (bounding box will be measured).")
    public int configAreaMaxWidth;

    @ConfigInt(def = 24, min = 8, desc = "Maximum portal area height (bounding box will be measured).")
    public int configAreaMaxHeight = 24;

    @ConfigInt(def = 128, min = 8, desc = "Maximum total amount of portal area blocks.")
    public int configAreaMaxBlocks = 128;

    @LangMessage
    public TranslatedMessage langSelectBoundaryNow;

    @LangMessage
    public TranslatedMessage langConsoleInvalidType;

    @LangMessage
    public TranslatedMessage langConsoleDifferentWorld;

    @LangMessage
    public TranslatedMessage langConsoleTooFarAway;

    @LangMessage
    public TranslatedMessage langConsoleLinked;

    @LangMessage
    public TranslatedMessage langNoBoundaryFound;

    @LangMessage
    public TranslatedMessage langNoOrigin;

    @LangMessage
    public TranslatedMessage langMultipleOrigins;

    @LangMessage
    public TranslatedMessage langNoPortalBlockAboveOrigin;

    @LangMessage
    public TranslatedMessage langNotEnoughPortalBlocksAboveOrigin;

    @LangMessage
    public TranslatedMessage langTooLarge;

    @LangMessage
    public TranslatedMessage langTooSmallSpawn;

    @LangMessage
    public TranslatedMessage langTooManyPortalAreaBlocks;

    @LangMessage
    public TranslatedMessage langPortalAreaObstructed;

    @LangMessage
    public TranslatedMessage langIntersectsExistingPortal;

    @LangMessage
    public TranslatedMessage langBuildRestricted;

    @LangMessage
    public TranslatedMessage langLinkRestricted;

    @LangMessage
    public TranslatedMessage langTargetAlreadyConnected;

    @LangMessage
    public TranslatedMessage langSourceUseRestricted;

    @LangMessage
    public TranslatedMessage langTargetUseRestricted;

    private Set<Material> portalBoundaryBuildMaterials = new HashSet<>();

    private HashMap<UUID, Block> pendingConsole = new HashMap<>();

    public PortalConstructor(Context<Portals> context) {
        super(context);
    }

    @Override
    public void onConfigChange() {
        portalBoundaryBuildMaterials.clear();
        portalBoundaryBuildMaterials.add(configMaterialBoundary1);
        portalBoundaryBuildMaterials.add(configMaterialBoundary2);
        portalBoundaryBuildMaterials.add(configMaterialBoundary3);
        portalBoundaryBuildMaterials.add(configMaterialBoundary4);
        portalBoundaryBuildMaterials.add(configMaterialBoundary5);
        portalBoundaryBuildMaterials.add(configMaterialOrigin);
    }

    public int maxDimX(Plane plane) {
        return plane.x() ? configAreaMaxWidth : 1;
    }

    public int maxDimY(Plane plane) {
        return plane.y() ? configAreaMaxHeight : 1;
    }

    public int maxDimZ(Plane plane) {
        return plane.z() ? configAreaMaxWidth : 1;
    }

    private boolean rememberNewConsole(final Player player, final Block consoleBlock) {
        final var changed = !consoleBlock.equals(pendingConsole.get(player.getUniqueId()));
        // Add consoleBlock as pending console
        pendingConsole.put(player.getUniqueId(), consoleBlock);
        if (changed) {
            langSelectBoundaryNow.send(player);
        }
        return changed;
    }

    private boolean canLinkConsole(
        final Player player,
        final PortalBoundary boundary,
        final Block console,
        boolean checkOnly
    ) {
        return canLinkConsole(player, boundary.allBlocks(), console, null, checkOnly);
    }

    private boolean canLinkConsole(
        final Player player,
        final Portal portal,
        final Block console,
        boolean checkOnly
    ) {
        // Gather all portal blocks that aren't consoles
        final var blocks = portal
            .blocks()
            .stream()
            .filter(pb -> pb.type() != PortalBlock.Type.CONSOLE)
            .map(pb -> pb.block())
            .collect(Collectors.toList());
        return canLinkConsole(player, blocks, console, portal, checkOnly);
    }

    private boolean canLinkConsole(
        final Player player,
        final List<Block> blocks,
        final Block console,
        @Nullable final Portal existingPortal,
        boolean checkOnly
    ) {
        // Check a console block type
        if (console.getType() != configMaterialConsole) {
            langConsoleInvalidType.send(player);
            return false;
        }

        // Check world
        if (!console.getWorld().equals(blocks.get(0).getWorld())) {
            langConsoleDifferentWorld.send(player);
            return false;
        }

        // Check distance
        boolean foundValidBlock = false;
        for (final var block : blocks) {
            if (
                Math.abs(console.getX() - block.getX()) <= configConsoleMaxDistanceXz &&
                Math.abs(console.getY() - block.getY()) <= configConsoleMaxDistanceY &&
                Math.abs(console.getZ() - block.getZ()) <= configConsoleMaxDistanceXz
            ) {
                foundValidBlock = true;
                break;
            }
        }

        if (!foundValidBlock) {
            langConsoleTooFarAway.send(player);
            return false;
        }

        // Call event
        final var event = new PortalLinkConsoleEvent(player, console, blocks, checkOnly, existingPortal);
        getModule().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() && !player.hasPermission(getModule().adminPermission)) {
            langLinkRestricted.send(player);
            return false;
        }

        return true;
    }

    private boolean linkConsole(final Player player, final Block console, final Portal portal) {
        if (!canLinkConsole(player, portal, console, false)) {
            return false;
        }

        // Add portal block
        getModule().addNewPortalBlock(portal, createPortalBlock(console));

        // Update block blocks
        portal.updateBlocks(getModule());
        return true;
    }

    private PortalBoundary findBoundary(final Player player, final Block block) {
        final var boundary = PortalBoundary.searchAt(this, block);
        if (boundary == null) {
            langNoBoundaryFound.send(player);
            return null;
        }

        // Check for error
        switch (boundary.errorState()) {
            case NONE:
                /* The Boundary is fine */break;
            case NO_ORIGIN:
                langNoOrigin.send(player);
                return null;
            case MULTIPLE_ORIGINS:
                langMultipleOrigins.send(player);
                return null;
            case NO_PORTAL_BLOCK_ABOVE_ORIGIN:
                langNoPortalBlockAboveOrigin.send(player);
                return null;
            case NOT_ENOUGH_PORTAL_BLOCKS_ABOVE_ORIGIN:
                langNotEnoughPortalBlocksAboveOrigin.send(player);
                return null;
            case TOO_LARGE_X:
                langTooLarge.send(player, "§6x");
                return null;
            case TOO_LARGE_Y:
                langTooLarge.send(player, "§6y");
                return null;
            case TOO_LARGE_Z:
                langTooLarge.send(player, "§6z");
                return null;
            case TOO_SMALL_SPAWN_X:
                langTooSmallSpawn.send(player, "§6x");
                return null;
            case TOO_SMALL_SPAWN_Y:
                langTooSmallSpawn.send(player, "§6y");
                return null;
            case TOO_SMALL_SPAWN_Z:
                langTooSmallSpawn.send(player, "§6z");
                return null;
            case PORTAL_AREA_OBSTRUCTED:
                langPortalAreaObstructed.send(player);
                return null;
            case TOO_MANY_PORTAL_AREA_BLOCKS:
                langTooManyPortalAreaBlocks.send(
                    player,
                    "§6" + boundary.portalAreaBlocks().size(),
                    "§6" + configAreaMaxBlocks
                );
                return null;
        }

        if (boundary.intersectsExistingPortal(this)) {
            langIntersectsExistingPortal.send(player);
            return null;
        }

        return boundary;
    }

    public boolean isTypePartOfBoundary(final Material material) {
        return (
            material == configMaterialBoundary1 ||
            material == configMaterialBoundary2 ||
            material == configMaterialBoundary3 ||
            material == configMaterialBoundary4 ||
            material == configMaterialBoundary5
        );
    }

    public boolean isTypePartOfBoundaryOrOrigin(final Material material) {
        return material == configMaterialOrigin || isTypePartOfBoundary(material);
    }

    private PortalBoundary checkConstructionConditions(
        final Player player,
        final Block console,
        final Block boundaryBlock,
        boolean checkOnly
    ) {
        if (getModule().isPortalBlock(boundaryBlock)) {
            getModule()
                .log.severe(
                    "constructPortal() was called on a boundary that already belongs to a portal! This is a bug."
                );
            return null;
        }

        // Search for valid portal boundary
        final var boundary = findBoundary(player, boundaryBlock);
        if (boundary == null) {
            return null;
        }

        // Check portal construct event
        final var event = new PortalConstructEvent(player, boundary, checkOnly);
        getModule().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            langBuildRestricted.send(player);
            return null;
        }

        // Check console distance and build permission
        if (!canLinkConsole(player, boundary, console, true)) {
            return null;
        }

        return boundary;
    }

    private PortalBlock createPortalBlock(final Block block) {
        final PortalBlock.Type type;
        var mat = block.getType();
        // treat cave air and void air as normal air
        if (mat == Material.CAVE_AIR || mat == Material.VOID_AIR) {
            mat = Material.AIR;
        }
        if (mat == configMaterialConsole) {
            type = PortalBlock.Type.CONSOLE;
        } else if (mat == configMaterialBoundary1) {
            type = PortalBlock.Type.BOUNDARY1;
        } else if (mat == configMaterialBoundary2) {
            type = PortalBlock.Type.BOUNDARY2;
        } else if (mat == configMaterialBoundary3) {
            type = PortalBlock.Type.BOUNDARY3;
        } else if (mat == configMaterialBoundary4) {
            type = PortalBlock.Type.BOUNDARY4;
        } else if (mat == configMaterialBoundary5) {
            type = PortalBlock.Type.BOUNDARY5;
        } else if (mat == configMaterialOrigin) {
            type = PortalBlock.Type.ORIGIN;
        } else if (mat == configMaterialPortalArea) {
            type = PortalBlock.Type.PORTAL;
        } else {
            getModule()
                .log.warning(
                    "Invalid block type '" +
                    mat +
                    "' encountered in portal block creation. Assuming boundary variant 1."
                );
            type = PortalBlock.Type.BOUNDARY1;
        }
        return new PortalBlock(block, type);
    }

    private boolean constructPortal(final Player player, final Block console, final Block boundaryBlock) {
        if (checkConstructionConditions(player, console, boundaryBlock, true) == null) {
            return false;
        }

        // Show name chooser
        getModule()
            .menus.enterNameMenu.create(player, (p, name) -> {
                // Re-check conditions, as someone could have changed blocks. This
                // prevents this race condition.
                final var boundary = checkConstructionConditions(p, console, boundaryBlock, false);
                if (boundary == null) {
                    return ClickResult.ERROR;
                }

                // Determine orientation
                final var orientation = Orientation.from(
                    boundary.plane(),
                    boundary.originBlock(),
                    console,
                    player.getLocation()
                );

                // Construct portal
                final var portal = new Portal(p.getUniqueId(), orientation, boundary.spawn());
                portal.name(name);
                getModule().addNewPortal(portal);

                // Add portal blocks
                for (final var block : boundary.allBlocks()) {
                    getModule().addNewPortalBlock(portal, createPortalBlock(block));
                }

                // Link console
                linkConsole(p, console, portal);

                // Force update storage now, as a precaution.
                getModule().updatePersistentData();

                // Update portal blocks once
                portal.updateBlocks(getModule());
                return ClickResult.SUCCESS;
            })
            .open(player);

        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractConsole(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final var block = event.getClickedBlock();
        if (block.getType() != configMaterialConsole) {
            return;
        }

        // Abort if the console belongs to another portal already.
        if (getModule().isPortalBlock(block)) {
            return;
        }

        // TODO portal stone as item instead of shifting?
        // Only if player sneak-right-clicks the console
        final var player = event.getPlayer();
        if (!player.isSneaking() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        rememberNewConsole(player, block);
        swingArm(player, event.getHand());
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onPlayerInteractBoundary(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final var block = event.getClickedBlock();
        final var portal = getModule().portalFor(block);
        final var type = block.getType();
        if (portal == null && !portalBoundaryBuildMaterials.contains(type)) {
            return;
        }

        // Break if no console is pending
        final var player = event.getPlayer();
        final var console = pendingConsole.remove(player.getUniqueId());
        if (console == null) {
            return;
        }

        if (portal == null) {
            if (constructPortal(player, console, block)) {
                swingArm(player, event.getHand());
            }
        } else {
            if (linkConsole(player, console, portal)) {
                swingArm(player, event.getHand());
            }
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }
}
