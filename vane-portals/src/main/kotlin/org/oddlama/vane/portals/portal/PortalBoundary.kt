package org.oddlama.vane.portals.portal

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.oddlama.vane.external.apache.commons.lang3.tuple.Pair
import org.oddlama.vane.portals.PortalConstructor
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class PortalBoundary private constructor(private val plane: Plane?) {
    enum class ErrorState {
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

    private var boundaryBlocks: MutableSet<Block>? = null
    private var portalAreaBlocks: MutableSet<Block>? = null

    // Origin block is the root block of the portal. It determines the search point
    // for the spawn location for XY and YZ types, and the region the portal belongs to.
    private var originBlock: Block? = null
    private var spawn: Location? = null

    private var errorState = ErrorState.NONE
    private var dimX = 0
    private var dimY = 0
    private var dimZ = 0

    // Returns all boundary blocks (excluding origin block)
    fun boundaryBlocks(): MutableSet<Block>? {
        return boundaryBlocks
    }

    // Returns all portal area blocks
    fun portalAreaBlocks(): MutableSet<Block>? {
        return portalAreaBlocks
    }

    /**
     * Returns the origin block (which is part of the portal outline but not included in
     * boundaryBlocks()). Can be null if no origin block was used but a portal shape was found
     */
    fun originBlock(): Block? {
        return originBlock
    }

    fun plane(): Plane? {
        return plane
    }

    fun spawn(): Location? {
        return spawn
    }

    fun errorState(): ErrorState {
        return errorState
    }

    fun dimX(): Int {
        return dimX
    }

    fun dimY(): Int {
        return dimY
    }

    fun dimZ(): Int {
        return dimZ
    }

    fun allBlocks(): MutableList<Block> {
        val allBlocks = ArrayList<Block>()
        allBlocks.addAll(boundaryBlocks!!)
        allBlocks.addAll(portalAreaBlocks!!)
        allBlocks.add(originBlock!!)
        return allBlocks
    }

    fun intersectsExistingPortal(portalConstructor: PortalConstructor): Boolean {
        for (b in allBlocks()) {
            if (portalConstructor.module!!.isPortalBlock(b)) {
                return true
            }
        }
        return false
    }

    override fun toString(): String {
        return "PortalBoundary{originBlock = $originBlock, plane = $plane}"
    }

    companion object {
        private fun pushBlockIfNotContained(
            block: Block?,
            stack: Stack<Block>,
            outBoundary: MutableSet<Block>,
            outPortalArea: MutableSet<Block>
        ) {
            if (block == null) {
                return
            }
            if (outBoundary.contains(block) || outPortalArea.contains(block)) {
                return
            }

            stack.push(block)
        }

        private fun pushAdjacentBlocksToStack(
            block: Block,
            stack: Stack<Block>,
            outBoundary: MutableSet<Block>,
            outPortalArea: MutableSet<Block>,
            plane: Plane
        ) {
            when (plane) {
                Plane.XY -> {
                    pushBlockIfNotContained(block.getRelative(1, 0, 0), stack, outBoundary, outPortalArea)
                    pushBlockIfNotContained(block.getRelative(-1, 0, 0), stack, outBoundary, outPortalArea)
                    pushBlockIfNotContained(block.getRelative(0, 1, 0), stack, outBoundary, outPortalArea)
                    pushBlockIfNotContained(block.getRelative(0, -1, 0), stack, outBoundary, outPortalArea)
                }

                Plane.YZ -> {
                    pushBlockIfNotContained(block.getRelative(0, 0, 1), stack, outBoundary, outPortalArea)
                    pushBlockIfNotContained(block.getRelative(0, 0, -1), stack, outBoundary, outPortalArea)
                    pushBlockIfNotContained(block.getRelative(0, 1, 0), stack, outBoundary, outPortalArea)
                    pushBlockIfNotContained(block.getRelative(0, -1, 0), stack, outBoundary, outPortalArea)
                }

                Plane.XZ -> {
                    pushBlockIfNotContained(block.getRelative(1, 0, 0), stack, outBoundary, outPortalArea)
                    pushBlockIfNotContained(block.getRelative(-1, 0, 0), stack, outBoundary, outPortalArea)
                    pushBlockIfNotContained(block.getRelative(0, 0, 1), stack, outBoundary, outPortalArea)
                    pushBlockIfNotContained(block.getRelative(0, 0, -1), stack, outBoundary, outPortalArea)
                }
            }
        }

        private fun doFloodFill4Step(
            portalConstructor: PortalConstructor,
            stack: Stack<Block>,
            outBoundary: MutableSet<Block>,
            outPortalArea: MutableSet<Block>,
            plane: Plane
        ) {
            val block = stack.pop()
            if (portalConstructor.isTypePartOfBoundaryOrOrigin(block.type)) {
                outBoundary.add(block)
            } else {
                outPortalArea.add(block)
                pushAdjacentBlocksToStack(block, stack, outBoundary, outPortalArea, plane)
            }
        }

        /**
         * Simultaneously fill two areas. Return as soon as a valid area is found or the maximum depth
         * is exceeded. Returns a pair of { boundary, portal_area }
         */
        private fun simultaneousFloodFill4(
            portalConstructor: PortalConstructor,
            areas: Array<Block?>,
            plane: Plane
        ): Pair<MutableSet<Block>?, MutableSet<Block>?>? {
            val boundary0: MutableSet<Block> = HashSet()
            val boundary1: MutableSet<Block> = HashSet()
            val portalArea0: MutableSet<Block> = HashSet()
            val portalArea1: MutableSet<Block> = HashSet()
            val floodFillStack0 = Stack<Block>()
            val floodFillStack1 = Stack<Block>()

            areas[0]?.let { floodFillStack0.push(it) }
            areas[1]?.let { floodFillStack1.push(it) }

            // Keep going as long as all stacks of enabled areas are not empty and max depth is not
            // reached
            var depth = 0
            while ((areas[0] == null || !floodFillStack0.isEmpty()) && (areas[1] == null || !floodFillStack1.isEmpty())
            ) {
                ++depth

                // Maximum depth reached -> both areas are invalid
                if (depth > portalConstructor.configAreaFloodfillMaxSteps) {
                    return null
                }

                if (areas[0] != null) {
                    doFloodFill4Step(portalConstructor, floodFillStack0, boundary0, portalArea0, plane)
                }
                if (areas[1] != null) {
                    doFloodFill4Step(portalConstructor, floodFillStack1, boundary1, portalArea1, plane)
                }
            }

            if (areas[0] != null && floodFillStack0.isEmpty()) {
                return Pair.of(boundary0, portalArea0)
            } else if (areas[1] != null && floodFillStack1.isEmpty()) {
                return Pair.of(boundary1, portalArea1)
            }

            // Cannot occur.
            return null
        }

        private fun getSurroundingBlocksCcw(block: Block, plane: Plane): MutableList<Block> {
            val surroundingBlocks = ArrayList<Block>()

            when (plane) {
                Plane.XY -> {
                    surroundingBlocks.add(block.getRelative(1, -1, 0))
                    surroundingBlocks.add(block.getRelative(1, 0, 0))
                    surroundingBlocks.add(block.getRelative(1, 1, 0))
                    surroundingBlocks.add(block.getRelative(0, 1, 0))
                    surroundingBlocks.add(block.getRelative(-1, 1, 0))
                    surroundingBlocks.add(block.getRelative(-1, 0, 0))
                    surroundingBlocks.add(block.getRelative(-1, -1, 0))
                    surroundingBlocks.add(block.getRelative(0, -1, 0))
                }

                Plane.YZ -> {
                    surroundingBlocks.add(block.getRelative(0, 1, -1))
                    surroundingBlocks.add(block.getRelative(0, 1, 0))
                    surroundingBlocks.add(block.getRelative(0, 1, 1))
                    surroundingBlocks.add(block.getRelative(0, 0, 1))
                    surroundingBlocks.add(block.getRelative(0, -1, 1))
                    surroundingBlocks.add(block.getRelative(0, -1, 0))
                    surroundingBlocks.add(block.getRelative(0, -1, -1))
                    surroundingBlocks.add(block.getRelative(0, 0, -1))
                }

                Plane.XZ -> {
                    surroundingBlocks.add(block.getRelative(1, 0, -1))
                    surroundingBlocks.add(block.getRelative(1, 0, 0))
                    surroundingBlocks.add(block.getRelative(1, 0, 1))
                    surroundingBlocks.add(block.getRelative(0, 0, 1))
                    surroundingBlocks.add(block.getRelative(-1, 0, 1))
                    surroundingBlocks.add(block.getRelative(-1, 0, 0))
                    surroundingBlocks.add(block.getRelative(-1, 0, -1))
                    surroundingBlocks.add(block.getRelative(0, 0, -1))
                }
            }

            return surroundingBlocks
        }

        private fun getPotentialAreaBlocks(
            portalConstructor: PortalConstructor,
            block: Block,
            plane: Plane
        ): Array<Block?>? {
            /* Step 1: Assert that the 8 surrounding blocks must include two or more boundary blocks
         * Step 2: Set area index to first area
         * Step 3: Start at any surrounding block.
         * Step 4: Check if the block is a boundary block
         *  - false: Set this as the start block for the current area.
         *           If both areas are assigned, stop here.
         *  - true: Set the area index to the other area (area + 1) % 2 if it is the first boundary block after a non boundary block
         * Step 5: Select next block CW/CCW. If it is the start block return the areas, else to step 4
         */

            val surroundingBlocks: MutableList<Block> = getSurroundingBlocksCcw(block, plane)

            // Assert that there are exactly two boundary blocks
            var boundaryBlocks = 0
            for (surroundingBlock in surroundingBlocks) {
                if (portalConstructor.isTypePartOfBoundaryOrOrigin(surroundingBlock.type)) {
                    ++boundaryBlocks
                }
            }

            if (boundaryBlocks < 2) {
                return null
            }

            // Identify areas
            val areas = arrayOfNulls<Block>(2)
            var areaIndex = 0
            var hadBoundaryBlockBefore = false
            for (surroundingBlock in surroundingBlocks) {
                // Examine a block type
                if (portalConstructor.isTypePartOfBoundaryOrOrigin(surroundingBlock.type)) {
                    if (!hadBoundaryBlockBefore) areaIndex = (areaIndex + 1) % 2

                    hadBoundaryBlockBefore = true
                } else {
                    areas[areaIndex] = surroundingBlock

                    // Check if another area is also set
                    if (areas[(areaIndex + 1) % 2] != null) return areas

                    hadBoundaryBlockBefore = false
                }
            }

            // Only less than two areas were found.
            return areas
        }

        private fun add3AirStacks(
            portalConstructor: PortalConstructor,
            startAir: Block,
            lowestAirBlocks: MutableList<Block>,
            insertFront: Boolean,
            modX: Int,
            modZ: Int
        ) {
            var air = startAir
            while (true) {
                air = air.getRelative(-modX, 0, -modZ)
                if (air.type != portalConstructor.configMaterialPortalArea) {
                    break
                }

                val boundary = air.getRelative(0, -1, 0)
                if (portalConstructor.isTypePartOfBoundary(boundary.type)) {
                    break
                }

                val above1 = air.getRelative(0, 1, 0)
                if (above1.type != portalConstructor.configMaterialPortalArea) {
                    break
                }

                val above2 = air.getRelative(0, 2, 0)
                if (above2.type != portalConstructor.configMaterialPortalArea) {
                    break
                }

                if (insertFront) {
                    lowestAirBlocks.add(0, air)
                } else {
                    lowestAirBlocks.add(air)
                }
            }
        }

        private fun searchAt(
            portalConstructor: PortalConstructor,
            searchBlock: Block,
            plane: Plane
        ): PortalBoundary? {
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

            val potentialAreaBlocks: Array<Block?>? = getPotentialAreaBlocks(portalConstructor, searchBlock, plane)

            // If potentialAreaBlocks is null, the shape is invalid
            if (potentialAreaBlocks == null || (potentialAreaBlocks[0] == null && potentialAreaBlocks[1] == null)) {
                return null
            }

            val result: Pair<MutableSet<Block>?, MutableSet<Block>?> =
                simultaneousFloodFill4(portalConstructor, potentialAreaBlocks, plane) ?: return null

            val boundary = PortalBoundary(plane)
            boundary.boundaryBlocks = result.getLeft()
            boundary.portalAreaBlocks = result.getRight()

            // Remove origin block from a boundary list
            val iterator = boundary.boundaryBlocks!!.iterator()
            while (iterator.hasNext()) {
                val block = iterator.next()
                if (block.type == portalConstructor.configMaterialOrigin) {
                    if (boundary.originBlock != null) {
                        // Duplicate origin block
                        boundary.errorState = ErrorState.MULTIPLE_ORIGINS
                        return boundary
                    } else {
                        iterator.remove()
                        boundary.originBlock = block
                    }
                }
            }

            // Check origin existence
            if (boundary.originBlock == null) {
                boundary.errorState = ErrorState.NO_ORIGIN
                return boundary
            }

            // Check area size
            if (boundary.portalAreaBlocks!!.size > portalConstructor.configAreaMaxBlocks) {
                boundary.errorState = ErrorState.TOO_MANY_PORTAL_AREA_BLOCKS
                return boundary
            }

            // Check maximum size constraints
            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var minZ = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var maxY = Int.MIN_VALUE
            var maxZ = Int.MIN_VALUE

            for (block in boundary.portalAreaBlocks!!) {
                minX = min(minX, block.x)
                minY = min(minY, block.y)
                minZ = min(minZ, block.z)
                maxX = max(maxX, block.x)
                maxY = max(maxY, block.y)
                maxZ = max(maxZ, block.z)
            }

            boundary.dimX = 1 + maxX - minX
            boundary.dimY = 1 + maxY - minY
            boundary.dimZ = 1 + maxZ - minZ

            if (boundary.dimX > portalConstructor.maxDimX(plane)) {
                boundary.errorState = ErrorState.TOO_LARGE_X
                return boundary
            } else if (boundary.dimY > portalConstructor.maxDimY(plane)) {
                boundary.errorState = ErrorState.TOO_LARGE_Y
                return boundary
            } else if (boundary.dimZ > portalConstructor.maxDimZ(plane)) {
                boundary.errorState = ErrorState.TOO_LARGE_Z
                return boundary
            }

            val airOverrides: Set<Material> = setOf(Material.CAVE_AIR, Material.AIR, Material.VOID_AIR)

            // Check area obstruction
            for (block in boundary.portalAreaBlocks!!) {
                if (block.type != portalConstructor.configMaterialPortalArea) {
                    if (portalConstructor.configMaterialPortalArea == Material.AIR) {
                        if (airOverrides.contains(block.type)) {
                            continue
                        }
                    }
                    boundary.errorState = ErrorState.PORTAL_AREA_OBSTRUCTED
                    return boundary
                }
            }

            // Determine spawn point and check minimum size constraints (these are only important at the
            // portal's spawn point)
            if (boundary.plane == Plane.XZ) {
                // Find middle of portal, then find first (2,3x2,3) (even,uneven) area in the direction
                // of origin block (the greater x/z distance is chosen)
                val middleSmallCoords = boundary
                    .originBlock()!!
                    .world
                    .getBlockAt(
                        floor(minX / 2.0 + maxX / 2.0).toInt(),
                        boundary.originBlock()!!.y,
                        floor(minZ / 2.0 + maxZ / 2.0).toInt()
                    )

                val diffX = middleSmallCoords.x - boundary.originBlock()!!.x
                val diffZ = middleSmallCoords.z - boundary.originBlock()!!.z

                // Calculate mod to add to middle block to get more into the direction of origin block
                val modX: Int
                val modZ: Int
                if (abs(diffX) > abs(diffZ)) {
                    modX = if (diffX > 0) -1 else 1
                    modZ = 0
                } else {
                    modX = 0
                    modZ = if (diffZ > 0) -1 else 1
                }

                val absModX = abs(modX)
                val absModZ = abs(modZ)

                // Find a strip of portal blocks last along the axis defined by the origin block.
                // If there are less than 2 blocks inside, the spawn area is too small.
                // Therefore, walk to origin until a block inside the area is found.
                var firstInside = middleSmallCoords
                while (!boundary.portalAreaBlocks!!.contains(firstInside)) {
                    if (modX != 0) {
                        if (firstInside.x !in (minX + 1)..<maxX) {
                            boundary.errorState = ErrorState.TOO_SMALL_SPAWN_X
                            return boundary
                        }
                    }

                    if (modZ != 0) {
                        if (firstInside.z !in (minZ + 1)..<maxZ) {
                            boundary.errorState = ErrorState.TOO_SMALL_SPAWN_Z
                            return boundary
                        }
                    }

                    firstInside = firstInside.getRelative(modX, 0, modZ)
                }

                // Find "backwards" last block inside
                var next = firstInside
                var backLastInside: Block?
                var totalBlocksInside = -1 // -1 because both forward and backward search includes firstInside
                do {
                    backLastInside = next
                    next = backLastInside.getRelative(-modX, 0, -modZ)
                    ++totalBlocksInside
                } while (boundary.portalAreaBlocks()!!.contains(next))

                // Find "forward" last block inside
                next = firstInside
                var lastInside: Block?
                do {
                    lastInside = next
                    next = lastInside.getRelative(modX, 0, modZ)
                    ++totalBlocksInside
                } while (boundary.portalAreaBlocks()!!.contains(next))

                if (totalBlocksInside < 2) {
                    boundary.errorState = if (modZ == 0) ErrorState.TOO_SMALL_SPAWN_X else ErrorState.TOO_SMALL_SPAWN_Z
                    return boundary
                }

                // Get block in the middle (if block edge round to smaller coords)
                val mX: Int
                val mZ: Int
                if (modX == 0) {
                    mX = floor(minX / 2.0 + maxX / 2.0).toInt()
                    mZ = floor(lastInside.z / 2.0 + backLastInside.z / 2.0).toInt()
                } else {
                    mX = floor(lastInside.x / 2.0 + backLastInside.x / 2.0).toInt()
                    mZ = floor(minZ / 2.0 + maxZ / 2.0).toInt()
                }

                val middleInside = lastInside.world.getBlockAt(mX, lastInside.y, mZ)

                // The origin axis will have its evenness determined by the number of connected air
                // blocks
                val evenAlongOriginAxis = totalBlocksInside % 2 == 0
                val evenAlongSideAxis = (if (modX == 0) boundary.dimZ else boundary.dimX) % 2 == 0

                val spawnOffsetAlongOriginAxis: Double
                val spawnOffsetAlongSideAxis: Double
                if (evenAlongOriginAxis) {
                    // Include coords of a middle plus one along origin direction
                    if (!boundary.portalAreaBlocks()!!.contains(middleInside.getRelative(absModX, 0, absModZ))) {
                        boundary.errorState =
                            if (modX == 0) ErrorState.TOO_SMALL_SPAWN_Z else ErrorState.TOO_SMALL_SPAWN_X
                        return boundary
                    }

                    spawnOffsetAlongOriginAxis = 1.0
                } else {
                    // Include coords of middle plus one and minus one along origin direction
                    if (!boundary.portalAreaBlocks()!!.contains(middleInside.getRelative(absModX, 0, absModZ))) {
                        boundary.errorState =
                            if (modX == 0) ErrorState.TOO_SMALL_SPAWN_Z else ErrorState.TOO_SMALL_SPAWN_X
                        return boundary
                    }

                    if (!boundary.portalAreaBlocks()!!.contains(middleInside.getRelative(-absModX, 0, -absModZ))) {
                        boundary.errorState =
                            if (modX == 0) ErrorState.TOO_SMALL_SPAWN_Z else ErrorState.TOO_SMALL_SPAWN_X
                        return boundary
                    }

                    spawnOffsetAlongOriginAxis = 0.5
                }

                if (evenAlongSideAxis) {
                    // Include coords of a middle plus one along a side direction
                    if (!boundary.portalAreaBlocks()!!.contains(middleInside.getRelative(absModZ, 0, absModX))) {
                        boundary.errorState =
                            if (modX == 0) ErrorState.TOO_SMALL_SPAWN_X else ErrorState.TOO_SMALL_SPAWN_Z
                        return boundary
                    }

                    spawnOffsetAlongSideAxis = 1.0
                } else {
                    // Include coords of middle plus and minus one along a side direction
                    if (!boundary.portalAreaBlocks()!!.contains(middleInside.getRelative(absModZ, 0, absModX))) {
                        boundary.errorState =
                            if (modX == 0) ErrorState.TOO_SMALL_SPAWN_X else ErrorState.TOO_SMALL_SPAWN_Z
                        return boundary
                    }

                    if (!boundary.portalAreaBlocks()!!.contains(middleInside.getRelative(-absModZ, 0, -absModX))) {
                        boundary.errorState =
                            if (modX == 0) ErrorState.TOO_SMALL_SPAWN_X else ErrorState.TOO_SMALL_SPAWN_Z
                        return boundary
                    }

                    spawnOffsetAlongSideAxis = 0.5
                }

                val spawnX: Double
                val spawnZ: Double
                if (modX == 0) {
                    spawnX = mX + spawnOffsetAlongSideAxis
                    spawnZ = mZ + spawnOffsetAlongOriginAxis
                } else {
                    spawnX = mX + spawnOffsetAlongOriginAxis
                    spawnZ = mZ + spawnOffsetAlongSideAxis
                }

                boundary.spawn = Location(middleInside.world, spawnX, middleInside.y + 0.5, spawnZ)
            } else {
                // Find the air (above) boundary (below) combinations at origin block, determine middle,
                // check if minimum size rectangle is part of the blocklist

                // The block above the origin block must be part of the portal

                val airAboveOrigin = boundary.originBlock()!!.getRelative(0, 1, 0)
                if (!boundary.portalAreaBlocks()!!.contains(airAboveOrigin)) {
                    boundary.errorState = ErrorState.NO_PORTAL_BLOCK_ABOVE_ORIGIN
                    return boundary
                }

                val airAboveWithBoundaryBelow = ArrayList<Block>()
                airAboveWithBoundaryBelow.add(airAboveOrigin)

                // Check for at least 1x3 air blocks above the origin
                val airAboveOrigin2 = boundary.originBlock()!!.getRelative(0, 2, 0)
                val airAboveOrigin3 = boundary.originBlock()!!.getRelative(0, 3, 0)
                if (!boundary.portalAreaBlocks()!!.contains(airAboveOrigin2) ||
                    !boundary.portalAreaBlocks()!!.contains(airAboveOrigin3)
                ) {
                    boundary.errorState = ErrorState.NOT_ENOUGH_PORTAL_BLOCKS_ABOVE_ORIGIN
                    return boundary
                }

                val modX = if (boundary.plane()!!.x()) 1 else 0
                val modZ = if (boundary.plane()!!.z()) 1 else 0

                // Find matching air stacks to negative axis side
                add3AirStacks(portalConstructor, airAboveOrigin, airAboveWithBoundaryBelow, false, -modX, -modZ)

                // Find matching pairs to positive axis side
                add3AirStacks(portalConstructor, airAboveOrigin, airAboveWithBoundaryBelow, true, modX, modZ)

                // Must be at least 1x3 area of portal blocks to be valid
                if (airAboveWithBoundaryBelow.isEmpty()) {
                    boundary.errorState = if (boundary.plane()!!.x())
                        ErrorState.TOO_SMALL_SPAWN_X
                    else
                        ErrorState.TOO_SMALL_SPAWN_Z
                    return boundary
                }

                // Spawn location is middle of air blocks
                val smallCoordEnd = airAboveWithBoundaryBelow[0]
                val largeCoordEnd = airAboveWithBoundaryBelow[airAboveWithBoundaryBelow.size - 1]
                val middleX = 0.5 + (smallCoordEnd.x + largeCoordEnd.x) / 2.0
                val middleZ = 0.5 + (smallCoordEnd.z + largeCoordEnd.z) / 2.0
                boundary.spawn = Location(
                    airAboveOrigin.world,
                    middleX,
                    airAboveOrigin.y + 0.05,
                    middleZ
                )
            }

            return boundary
        }

        @JvmStatic
        fun searchAt(portalConstructor: PortalConstructor, block: Block): PortalBoundary? {
            var boundary: PortalBoundary? = searchAt(portalConstructor, block, Plane.XY)
            if (boundary != null) {
                return boundary
            }

            boundary = searchAt(portalConstructor, block, Plane.YZ)
            if (boundary != null) {
                return boundary
            }

            return searchAt(portalConstructor, block, Plane.XZ)
        }
    }
}
