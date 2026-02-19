package org.oddlama.vane.portals.portal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import org.oddlama.vane.external.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.oddlama.vane.portals.PortalConstructor;

public class PortalBoundary {

    public static enum ErrorState {
        NONE,
        NO_ORIGIN,
        MULTIPLE_ORIGINS,
        TOO_SMALL_SPAWN_X,
        TOO_SMALL_SPAWN_Y,
        TOO_SMALL_SPAWN_Z,
        TOO_LARGE_X,
        TOO_LARGE_Y,
        TOO_LARGE_Z,
        NO_PORTAL_BLOCK_ABOVE_ORIGIN,
        NOT_ENOUGH_PORTAL_BLOCKS_ABOVE_ORIGIN,
        TOO_MANY_PORTAL_AREA_BLOCKS,
        PORTAL_AREA_OBSTRUCTED,
    }

    private Set<Block> boundaryBlocks = null;
    private Set<Block> portalAreaBlocks = null;

    // Origin block is the root block of the portal. It determines the search point
    // for the spawn location for XY and YZ types, and the region the portal belongs to.
    private Block originBlock = null;
    private final Plane plane;
    private Location spawn = null;

    private ErrorState errorState = ErrorState.NONE;
    private int dimX, dimY, dimZ;

    private PortalBoundary(Plane plane) {
        this.plane = plane;
    }

    // Returns all boundary blocks (excluding origin block)
    public Set<Block> boundaryBlocks() {
        return boundaryBlocks;
    }

    // Returns all portal area blocks
    public Set<Block> portalAreaBlocks() {
        return portalAreaBlocks;
    }

    /**
     * Returns the origin block (which is part of the portal outline but not included in
     * boundaryBlocks()). Can be null if no origin block was used but a portal shape was found
     */
    public Block originBlock() {
        return originBlock;
    }

    public Plane plane() {
        return plane;
    }

    public Location spawn() {
        return spawn;
    }

    public ErrorState errorState() {
        return errorState;
    }

    public int dimX() {
        return dimX;
    }

    public int dimY() {
        return dimY;
    }

    public int dimZ() {
        return dimZ;
    }

    public List<Block> allBlocks() {
        final var allBlocks = new ArrayList<Block>();
        allBlocks.addAll(boundaryBlocks);
        allBlocks.addAll(portalAreaBlocks);
        allBlocks.add(originBlock);
        return allBlocks;
    }

    public boolean intersectsExistingPortal(final PortalConstructor portalConstructor) {
        for (final var b : allBlocks()) {
            if (portalConstructor.getModule().isPortalBlock(b)) {
                return true;
            }
        }
        return false;
    }

    private static void pushBlockIfNotContained(
        final Block block,
        final Stack<Block> stack,
        final Set<Block> outBoundary,
        final Set<Block> outPortalArea
    ) {
        if (outBoundary.contains(block) || outPortalArea.contains(block)) {
            return;
        }

        stack.push(block);
    }

    private static void pushAdjacentBlocksToStack(
        final Block block,
        final Stack<Block> stack,
        final Set<Block> outBoundary,
        final Set<Block> outPortalArea,
        Plane plane
    ) {
        switch (plane) {
            case XY:
                pushBlockIfNotContained(block.getRelative(1, 0, 0), stack, outBoundary, outPortalArea);
                pushBlockIfNotContained(block.getRelative(-1, 0, 0), stack, outBoundary, outPortalArea);
                pushBlockIfNotContained(block.getRelative(0, 1, 0), stack, outBoundary, outPortalArea);
                pushBlockIfNotContained(block.getRelative(0, -1, 0), stack, outBoundary, outPortalArea);
                break;
            case YZ:
                pushBlockIfNotContained(block.getRelative(0, 0, 1), stack, outBoundary, outPortalArea);
                pushBlockIfNotContained(block.getRelative(0, 0, -1), stack, outBoundary, outPortalArea);
                pushBlockIfNotContained(block.getRelative(0, 1, 0), stack, outBoundary, outPortalArea);
                pushBlockIfNotContained(block.getRelative(0, -1, 0), stack, outBoundary, outPortalArea);
                break;
            case XZ:
                pushBlockIfNotContained(block.getRelative(1, 0, 0), stack, outBoundary, outPortalArea);
                pushBlockIfNotContained(block.getRelative(-1, 0, 0), stack, outBoundary, outPortalArea);
                pushBlockIfNotContained(block.getRelative(0, 0, 1), stack, outBoundary, outPortalArea);
                pushBlockIfNotContained(block.getRelative(0, 0, -1), stack, outBoundary, outPortalArea);
                break;
        }
    }

    private static void doFloodFill4Step(
        final PortalConstructor portalConstructor,
        final Stack<Block> stack,
        final Set<Block> outBoundary,
        final Set<Block> outPortalArea,
        final Plane plane
    ) {
        final var block = stack.pop();
        if (portalConstructor.isTypePartOfBoundaryOrOrigin(block.getType())) {
            outBoundary.add(block);
        } else {
            outPortalArea.add(block);
            pushAdjacentBlocksToStack(block, stack, outBoundary, outPortalArea, plane);
        }
    }

    /**
     * Simultaneously fill two areas. Return as soon as a valid area is found or the maximum depth
     * is exceeded. Returns a pair of { boundary, portal_area }
     */
    private static Pair<Set<Block>, Set<Block>> simultaneousFloodFill4(
        final PortalConstructor portalConstructor,
        final Block[] areas,
        final Plane plane
    ) {
        final var boundary0 = new HashSet<Block>();
        final var boundary1 = new HashSet<Block>();
        final var portalArea0 = new HashSet<Block>();
        final var portalArea1 = new HashSet<Block>();
        final var floodFillStack0 = new Stack<Block>();
        final var floodFillStack1 = new Stack<Block>();

        if (areas[0] != null) {
            floodFillStack0.push(areas[0]);
        }
        if (areas[1] != null) {
            floodFillStack1.push(areas[1]);
        }

        // Keep going as long as all stacks of enabled areas are not empty and max depth is not
        // reached
        int depth = 0;
        while (
            (areas[0] == null || !floodFillStack0.isEmpty()) && (areas[1] == null || !floodFillStack1.isEmpty())
        ) {
            ++depth;

            // Maximum depth reached -> both areas are invalid
            if (depth > portalConstructor.configAreaFloodfillMaxSteps) {
                return null;
            }

            if (areas[0] != null) {
                doFloodFill4Step(portalConstructor, floodFillStack0, boundary0, portalArea0, plane);
            }
            if (areas[1] != null) {
                doFloodFill4Step(portalConstructor, floodFillStack1, boundary1, portalArea1, plane);
            }
        }

        if (areas[0] != null && floodFillStack0.isEmpty()) {
            return Pair.of(boundary0, portalArea0);
        } else if (areas[1] != null && floodFillStack1.isEmpty()) {
            return Pair.of(boundary1, portalArea1);
        }

        // Cannot occur.
        return null;
    }

    private static List<Block> getSurroundingBlocksCcw(final Block block, final Plane plane) {
        final var surroundingBlocks = new ArrayList<Block>();

        switch (plane) {
            case XY:
                surroundingBlocks.add(block.getRelative(1, -1, 0));
                surroundingBlocks.add(block.getRelative(1, 0, 0));
                surroundingBlocks.add(block.getRelative(1, 1, 0));
                surroundingBlocks.add(block.getRelative(0, 1, 0));
                surroundingBlocks.add(block.getRelative(-1, 1, 0));
                surroundingBlocks.add(block.getRelative(-1, 0, 0));
                surroundingBlocks.add(block.getRelative(-1, -1, 0));
                surroundingBlocks.add(block.getRelative(0, -1, 0));
                break;
            case YZ:
                surroundingBlocks.add(block.getRelative(0, 1, -1));
                surroundingBlocks.add(block.getRelative(0, 1, 0));
                surroundingBlocks.add(block.getRelative(0, 1, 1));
                surroundingBlocks.add(block.getRelative(0, 0, 1));
                surroundingBlocks.add(block.getRelative(0, -1, 1));
                surroundingBlocks.add(block.getRelative(0, -1, 0));
                surroundingBlocks.add(block.getRelative(0, -1, -1));
                surroundingBlocks.add(block.getRelative(0, 0, -1));
                break;
            case XZ:
                surroundingBlocks.add(block.getRelative(1, 0, -1));
                surroundingBlocks.add(block.getRelative(1, 0, 0));
                surroundingBlocks.add(block.getRelative(1, 0, 1));
                surroundingBlocks.add(block.getRelative(0, 0, 1));
                surroundingBlocks.add(block.getRelative(-1, 0, 1));
                surroundingBlocks.add(block.getRelative(-1, 0, 0));
                surroundingBlocks.add(block.getRelative(-1, 0, -1));
                surroundingBlocks.add(block.getRelative(0, 0, -1));
                break;
        }

        return surroundingBlocks;
    }

    private static Block[] getPotentialAreaBlocks(
        final PortalConstructor portalConstructor,
        final Block block,
        final Plane plane
    ) {
        /* Step 1: Assert that the 8 surrounding blocks must include two or more boundary blocks
         * Step 2: Set area index to first area
         * Step 3: Start at any surrounding block.
         * Step 4: Check if the block is a boundary block
         *  - false: Set this as the start block for the current area.
         *           If both areas are assigned, stop here.
         *  - true: Set the area index to the other area (area + 1) % 2 if it is the first boundary block after a non boundary block
         * Step 5: Select next block CW/CCW. If it is the start block return the areas, else to step 4
         */

        final var surroundingBlocks = getSurroundingBlocksCcw(block, plane);

        // Assert that there are exactly two boundary blocks
        int boundaryBlocks = 0;
        for (final var surroundingBlock : surroundingBlocks) {
            if (portalConstructor.isTypePartOfBoundaryOrOrigin(surroundingBlock.getType())) {
                ++boundaryBlocks;
            }
        }

        if (boundaryBlocks < 2) {
            return null;
        }

        // Identify areas
        final var areas = new Block[2];
        int areaIndex = 0;
        boolean hadBoundaryBlockBefore = false;
        for (final var surroundingBlock : surroundingBlocks) {
            // Examine a block type
            if (portalConstructor.isTypePartOfBoundaryOrOrigin(surroundingBlock.getType())) {
                if (!hadBoundaryBlockBefore) areaIndex = (areaIndex + 1) % 2;

                hadBoundaryBlockBefore = true;
            } else {
                areas[areaIndex] = surroundingBlock;

                // Check if another area is also set
                if (areas[(areaIndex + 1) % 2] != null) return areas;

                hadBoundaryBlockBefore = false;
            }
        }

        // Only less than two areas were found.
        return areas;
    }

    private static void add3AirStacks(
        final PortalConstructor portalConstructor,
        final Block startAir,
        final List<Block> lowestAirBlocks,
        boolean insertFront,
        int modX,
        int modZ
    ) {
        var air = startAir;
        while (true) {
            air = air.getRelative(-modX, 0, -modZ);
            if (air.getType() != portalConstructor.configMaterialPortalArea) {
                break;
            }

            final var boundary = air.getRelative(0, -1, 0);
            if (portalConstructor.isTypePartOfBoundary(boundary.getType())) {
                break;
            }

            final var above1 = air.getRelative(0, 1, 0);
            if (above1.getType() != portalConstructor.configMaterialPortalArea) {
                break;
            }

            final var above2 = air.getRelative(0, 2, 0);
            if (above2.getType() != portalConstructor.configMaterialPortalArea) {
                break;
            }

            if (insertFront) {
                lowestAirBlocks.add(0, air);
            } else {
                lowestAirBlocks.add(air);
            }
        }
    }

    private static PortalBoundary searchAt(
        final PortalConstructor portalConstructor,
        final Block searchBlock,
        Plane plane
    ) {
        /* A 3x3 field of blocks around the start block is always split into 2 areas:
         *
         * []''''
         * ..##[]
         * ......
         *
         * [] = boundary
         * ## = start boundary block
         * .. = area 1
         * '' = area 2
         *
         * Anything else is invalid.
         *
         * Step 1: Determine one block each, for area 1 and area 2
         * Step 2: Do a flood fill algorithm at the same time on both areas
         *  - The one that finishes first wins
         *  - If both exceed (two times) the max block count, it is invalid
         * Result: The flood fill algorithm returns the boundary and portal area for the valid area or null if both are invalid.
         */

        final var potentialAreaBlocks = getPotentialAreaBlocks(portalConstructor, searchBlock, plane);

        // If potentialAreaBlocks is null, the shape is invalid
        if (potentialAreaBlocks == null || (potentialAreaBlocks[0] == null && potentialAreaBlocks[1] == null)) {
            return null;
        }

        final var result = simultaneousFloodFill4(portalConstructor, potentialAreaBlocks, plane);
        if (result == null) {
            return null;
        }

        final var boundary = new PortalBoundary(plane);
        boundary.boundaryBlocks = result.getLeft();
        boundary.portalAreaBlocks = result.getRight();

        // Remove origin block from a boundary list
        final var iterator = boundary.boundaryBlocks.iterator();
        while (iterator.hasNext()) {
            final var block = iterator.next();
            if (block.getType() == portalConstructor.configMaterialOrigin) {
                if (boundary.originBlock != null) {
                    // Duplicate origin block
                    boundary.errorState = ErrorState.MULTIPLE_ORIGINS;
                    return boundary;
                } else {
                    iterator.remove();
                    boundary.originBlock = block;
                }
            }
        }

        // Check origin existence
        if (boundary.originBlock == null) {
            boundary.errorState = ErrorState.NO_ORIGIN;
            return boundary;
        }

        // Check area size
        if (boundary.portalAreaBlocks.size() > portalConstructor.configAreaMaxBlocks) {
            boundary.errorState = ErrorState.TOO_MANY_PORTAL_AREA_BLOCKS;
            return boundary;
        }

        // Check maximum size constraints
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (final var block : boundary.portalAreaBlocks) {
            minX = Math.min(minX, block.getX());
            minY = Math.min(minY, block.getY());
            minZ = Math.min(minZ, block.getZ());
            maxX = Math.max(maxX, block.getX());
            maxY = Math.max(maxY, block.getY());
            maxZ = Math.max(maxZ, block.getZ());
        }

        boundary.dimX = 1 + maxX - minX;
        boundary.dimY = 1 + maxY - minY;
        boundary.dimZ = 1 + maxZ - minZ;

        if (boundary.dimX > portalConstructor.maxDimX(plane)) {
            boundary.errorState = ErrorState.TOO_LARGE_X;
            return boundary;
        } else if (boundary.dimY > portalConstructor.maxDimY(plane)) {
            boundary.errorState = ErrorState.TOO_LARGE_Y;
            return boundary;
        } else if (boundary.dimZ > portalConstructor.maxDimZ(plane)) {
            boundary.errorState = ErrorState.TOO_LARGE_Z;
            return boundary;
        }

        Set<Material> airOverrides = Set.of(Material.CAVE_AIR, Material.AIR, Material.VOID_AIR);

        // Check area obstruction
        for (final var block : boundary.portalAreaBlocks) {
            if (block.getType() != portalConstructor.configMaterialPortalArea) {
                if (portalConstructor.configMaterialPortalArea == Material.AIR) {
                    if (airOverrides.contains(block.getType())) {
                        continue;
                    }
                }
                boundary.errorState = ErrorState.PORTAL_AREA_OBSTRUCTED;
                return boundary;
            }
        }

        // Determine spawn point and check minimum size constraints (these are only important at the
        // portal's spawn point)
        if (boundary.plane == Plane.XZ) {
            // Find middle of portal, then find first (2,3x2,3) (even,uneven) area in the direction
            // of origin block (the greater x/z distance is chosen)
            final var middleSmallCoords = boundary
                .originBlock()
                .getWorld()
                .getBlockAt(
                    (int) Math.floor(minX / 2.0 + maxX / 2.0),
                    boundary.originBlock().getY(),
                    (int) Math.floor(minZ / 2.0 + maxZ / 2.0)
                );

            int diffX = middleSmallCoords.getX() - boundary.originBlock().getX();
            int diffZ = middleSmallCoords.getZ() - boundary.originBlock().getZ();

            // Calculate mod to add to middle block to get more into the direction of origin block
            int modX, modZ;
            int absModX, absModZ;
            if (Math.abs(diffX) > Math.abs(diffZ)) {
                modX = diffX > 0 ? -1 : 1;
                absModX = 1;
                modZ = absModZ = 0;
            } else {
                modX = absModX = 0;
                absModZ = 1;
                modZ = diffZ > 0 ? -1 : 1;
            }

            // Find a strip of portal blocks last along the axis defined by the origin block.
            // If there are less than 2 blocks inside, the spawn area is too small.
            // Therefore, walk to origin until a block inside the area is found.
            var firstInside = middleSmallCoords;
            while (!boundary.portalAreaBlocks.contains(firstInside)) {
                if (modX != 0) {
                    if (firstInside.getX() <= minX || firstInside.getX() >= maxX) {
                        boundary.errorState = ErrorState.TOO_SMALL_SPAWN_X;
                        return boundary;
                    }
                }

                if (modZ != 0) {
                    if (firstInside.getZ() <= minZ || firstInside.getZ() >= maxZ) {
                        boundary.errorState = ErrorState.TOO_SMALL_SPAWN_Z;
                        return boundary;
                    }
                }

                firstInside = firstInside.getRelative(modX, 0, modZ);
            }

            // Find "backwards" last block inside
            Block next = firstInside;
            Block backLastInside;
            int totalBlocksInside = -1; // -1 because both forward and backward search includes firstInside
            do {
                backLastInside = next;
                next = backLastInside.getRelative(-modX, 0, -modZ);
                ++totalBlocksInside;
            } while (boundary.portalAreaBlocks().contains(next));

            // Find "forward" last block inside
            next = firstInside;
            Block lastInside;
            do {
                lastInside = next;
                next = lastInside.getRelative(modX, 0, modZ);
                ++totalBlocksInside;
            } while (boundary.portalAreaBlocks().contains(next));

            if (totalBlocksInside < 2) {
                boundary.errorState = modZ == 0 ? ErrorState.TOO_SMALL_SPAWN_X : ErrorState.TOO_SMALL_SPAWN_Z;
                return boundary;
            }

            // Get block in the middle (if block edge round to smaller coords)
            int mX, mZ;
            if (modX == 0) {
                mX = (int) Math.floor(minX / 2.0 + maxX / 2.0);
                mZ = (int) Math.floor(lastInside.getZ() / 2.0 + backLastInside.getZ() / 2.0);
            } else {
                mX = (int) Math.floor(lastInside.getX() / 2.0 + backLastInside.getX() / 2.0);
                mZ = (int) Math.floor(minZ / 2.0 + maxZ / 2.0);
            }

            final var middleInside = lastInside.getWorld().getBlockAt(mX, lastInside.getY(), mZ);

            // The origin axis will have its evenness determined by the number of connected air
            // blocks
            boolean evenAlongOriginAxis = totalBlocksInside % 2 == 0;
            boolean evenAlongSideAxis = (modX == 0 ? boundary.dimZ : boundary.dimX) % 2 == 0;

            double spawnOffsetAlongOriginAxis;
            double spawnOffsetAlongSideAxis;
            if (evenAlongOriginAxis) {
                // Include coords of a middle plus one along origin direction
                if (!boundary.portalAreaBlocks().contains(middleInside.getRelative(absModX, 0, absModZ))) {
                    boundary.errorState = modX == 0 ? ErrorState.TOO_SMALL_SPAWN_Z : ErrorState.TOO_SMALL_SPAWN_X;
                    return boundary;
                }

                spawnOffsetAlongOriginAxis = 1.0;
            } else {
                // Include coords of middle plus one and minus one along origin direction
                if (!boundary.portalAreaBlocks().contains(middleInside.getRelative(absModX, 0, absModZ))) {
                    boundary.errorState = modX == 0 ? ErrorState.TOO_SMALL_SPAWN_Z : ErrorState.TOO_SMALL_SPAWN_X;
                    return boundary;
                }

                if (!boundary.portalAreaBlocks().contains(middleInside.getRelative(-absModX, 0, -absModZ))) {
                    boundary.errorState = modX == 0 ? ErrorState.TOO_SMALL_SPAWN_Z : ErrorState.TOO_SMALL_SPAWN_X;
                    return boundary;
                }

                spawnOffsetAlongOriginAxis = 0.5;
            }

            if (evenAlongSideAxis) {
                // Include coords of a middle plus one along a side direction
                if (!boundary.portalAreaBlocks().contains(middleInside.getRelative(absModZ, 0, absModX))) {
                    boundary.errorState = modX == 0 ? ErrorState.TOO_SMALL_SPAWN_X : ErrorState.TOO_SMALL_SPAWN_Z;
                    return boundary;
                }

                spawnOffsetAlongSideAxis = 1.0;
            } else {
                // Include coords of middle plus and minus one along a side direction
                if (!boundary.portalAreaBlocks().contains(middleInside.getRelative(absModZ, 0, absModX))) {
                    boundary.errorState = modX == 0 ? ErrorState.TOO_SMALL_SPAWN_X : ErrorState.TOO_SMALL_SPAWN_Z;
                    return boundary;
                }

                if (!boundary.portalAreaBlocks().contains(middleInside.getRelative(-absModZ, 0, -absModX))) {
                    boundary.errorState = modX == 0 ? ErrorState.TOO_SMALL_SPAWN_X : ErrorState.TOO_SMALL_SPAWN_Z;
                    return boundary;
                }

                spawnOffsetAlongSideAxis = 0.5;
            }

            double spawnX, spawnZ;
            if (modX == 0) {
                spawnX = mX + spawnOffsetAlongSideAxis;
                spawnZ = mZ + spawnOffsetAlongOriginAxis;
            } else {
                spawnX = mX + spawnOffsetAlongOriginAxis;
                spawnZ = mZ + spawnOffsetAlongSideAxis;
            }

            boundary.spawn = new Location(middleInside.getWorld(), spawnX, middleInside.getY() + 0.5, spawnZ);
        } else {
            // Find the air (above) boundary (below) combinations at origin block, determine middle,
            // check if minimum size rectangle is part of the blocklist

            // The block above the origin block must be part of the portal
            final var airAboveOrigin = boundary.originBlock().getRelative(0, 1, 0);
            if (!boundary.portalAreaBlocks().contains(airAboveOrigin)) {
                boundary.errorState = ErrorState.NO_PORTAL_BLOCK_ABOVE_ORIGIN;
                return boundary;
            }

            final var airAboveWithBoundaryBelow = new ArrayList<Block>();
            airAboveWithBoundaryBelow.add(airAboveOrigin);

            // Check for at least 1x3 air blocks above the origin
            final var airAboveOrigin2 = boundary.originBlock().getRelative(0, 2, 0);
            final var airAboveOrigin3 = boundary.originBlock().getRelative(0, 3, 0);
            if (
                !boundary.portalAreaBlocks().contains(airAboveOrigin2) ||
                !boundary.portalAreaBlocks().contains(airAboveOrigin3)
            ) {
                boundary.errorState = ErrorState.NOT_ENOUGH_PORTAL_BLOCKS_ABOVE_ORIGIN;
                return boundary;
            }

            int modX = boundary.plane().x() ? 1 : 0;
            int modZ = boundary.plane().z() ? 1 : 0;

            // Find matching air stacks to negative axis side
            add3AirStacks(portalConstructor, airAboveOrigin, airAboveWithBoundaryBelow, false, -modX, -modZ);

            // Find matching pairs to positive axis side
            add3AirStacks(portalConstructor, airAboveOrigin, airAboveWithBoundaryBelow, true, modX, modZ);

            // Must be at least 1x3 area of portal blocks to be valid
            if (airAboveWithBoundaryBelow.size() < 1) {
                boundary.errorState = boundary.plane().x()
                    ? ErrorState.TOO_SMALL_SPAWN_X
                    : ErrorState.TOO_SMALL_SPAWN_Z;
                return boundary;
            }

            // Spawn location is middle of air blocks
            final var smallCoordEnd = airAboveWithBoundaryBelow.get(0);
            final var largeCoordEnd = airAboveWithBoundaryBelow.get(airAboveWithBoundaryBelow.size() - 1);
            final var middleX = 0.5 + (smallCoordEnd.getX() + largeCoordEnd.getX()) / 2.0;
            final var middleZ = 0.5 + (smallCoordEnd.getZ() + largeCoordEnd.getZ()) / 2.0;
            boundary.spawn = new Location(
                airAboveOrigin.getWorld(),
                middleX,
                airAboveOrigin.getY() + 0.05,
                middleZ
            );
        }

        return boundary;
    }

    public static PortalBoundary searchAt(final PortalConstructor portalConstructor, final Block block) {
        var boundary = searchAt(portalConstructor, block, Plane.XY);
        if (boundary != null) {
            return boundary;
        }

        boundary = searchAt(portalConstructor, block, Plane.YZ);
        if (boundary != null) {
            return boundary;
        }

        return searchAt(portalConstructor, block, Plane.XZ);
    }

    @Override
    public String toString() {
        return "PortalBoundary{originBlock = " + originBlock + ", plane = " + plane + "}";
    }
}
