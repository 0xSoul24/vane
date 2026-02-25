package org.oddlama.vane.util

import net.minecraft.core.BlockPos
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Skull
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.util.BlockVector
import org.bukkit.util.Vector
import java.util.Collections

object BlockUtil {
    @JvmField
    val BLOCK_FACES: MutableList<BlockFace?> = mutableListOf(
        BlockFace.NORTH,
        BlockFace.EAST,
        BlockFace.SOUTH,
        BlockFace.WEST,
        BlockFace.UP,
        BlockFace.DOWN
    )

    @JvmField
    val XZ_FACES: MutableList<BlockFace> = mutableListOf(
        BlockFace.NORTH,
        BlockFace.EAST,
        BlockFace.SOUTH,
        BlockFace.WEST
    )

    const val NEAREST_RELATIVE_BLOCKS_FOR_RADIUS_MAX: Int = 6
    @JvmField
    val NEAREST_RELATIVE_BLOCKS_FOR_RADIUS: MutableList<MutableList<BlockVector>> =
        ArrayList()

    init {
        for (i in 0..NEAREST_RELATIVE_BLOCKS_FOR_RADIUS_MAX) {
            NEAREST_RELATIVE_BLOCKS_FOR_RADIUS.add(nearestBlocksForRadius(i))
        }
    }

    fun equalsPos(b1: Block, b2: Block): Boolean {
        return b1.x == b2.x && b1.y == b2.y && b1.z == b2.z
    }

    @JvmStatic
    fun dropNaturally(block: Block, drop: ItemStack) {
        dropNaturally(block.location.add(0.5, 0.5, 0.5), drop)
    }

    @JvmStatic
    fun dropNaturally(loc: Location, drop: ItemStack) {
        loc
            .getWorld()
            .dropItem(loc.add(Vector.getRandom().subtract(Vector(.5, .5, .5)).multiply(0.5)), drop).velocity =
            Vector.getRandom().add(Vector(-.5, +.5, -.5)).normalize().multiply(.15)
    }

    fun nearestBlocksForRadius(radius: Int): MutableList<BlockVector> {
        val ret = ArrayList<BlockVector>()

        // Use square bounding box
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                // Only circular area
                if (x * x + z * z > radius * radius + 0.5) {
                    continue
                }

                ret.add(BlockVector(x, 0, z))
            }
        }

        Collections.sort(ret, BlockVectorRadiusComparator())
        return ret
    }

    @JvmStatic
    fun relative(block: Block, relative: Vector): Block {
        return block.getRelative(relative.blockX, relative.blockY, relative.blockZ)
    }

    @JvmStatic
    fun nextTillableBlock(rootBlock: Block, radius: Int, careless: Boolean): Block? {
        for (relativePos in NEAREST_RELATIVE_BLOCKS_FOR_RADIUS[radius]) {
            val block = relative(rootBlock, relativePos)

            // Check for a tillable material
            if (!MaterialUtil.isTillable(block.type)) {
                continue
            }

            // Get block above
            val above = block.getRelative(0, 1, 0)

            if (above.type == Material.AIR) {
                // If the block above is air, we can till the block.
                return block
            } else if (careless && MaterialUtil.isReplaceableGrass(above.type)) {
                // If the block above is replaceable grass, delete it and return the block.
                above.type = Material.AIR
                return block
            }
        }

        // We are outside the radius.
        return null
    }

    @JvmStatic
    fun adjacentBlocks3D(root: Block): Array<Block?> {
        val adjacent = arrayOfNulls<Block>(26)

        // Direct adjacent
        adjacent[0] = root.getRelative(1, 0, 0)
        adjacent[1] = root.getRelative(-1, 0, 0)
        adjacent[2] = root.getRelative(0, 0, 1)
        adjacent[3] = root.getRelative(0, 0, -1)
        adjacent[4] = root.getRelative(0, 1, 0)
        adjacent[5] = root.getRelative(0, -1, 0)

        // Edge adjacent
        adjacent[6] = root.getRelative(0, -1, -1)
        adjacent[7] = root.getRelative(0, -1, 1)
        adjacent[8] = root.getRelative(0, 1, -1)
        adjacent[9] = root.getRelative(0, 1, 1)
        adjacent[10] = root.getRelative(-1, 0, -1)
        adjacent[11] = root.getRelative(-1, 0, 1)
        adjacent[12] = root.getRelative(1, 0, -1)
        adjacent[13] = root.getRelative(1, 0, 1)
        adjacent[14] = root.getRelative(-1, -1, 0)
        adjacent[15] = root.getRelative(-1, 1, 0)
        adjacent[16] = root.getRelative(1, -1, 0)
        adjacent[17] = root.getRelative(1, 1, 0)

        // Corner adjacent
        adjacent[18] = root.getRelative(-1, -1, -1)
        adjacent[19] = root.getRelative(-1, -1, 1)
        adjacent[20] = root.getRelative(-1, 1, -1)
        adjacent[21] = root.getRelative(-1, 1, 1)
        adjacent[22] = root.getRelative(1, -1, -1)
        adjacent[23] = root.getRelative(1, -1, 1)
        adjacent[24] = root.getRelative(1, 1, -1)
        adjacent[25] = root.getRelative(1, 1, 1)

        return adjacent
    }

    @JvmStatic
    fun nextSeedableBlock(rootBlock: Block, farmlandType: Material?, radius: Int): Block? {
        for (relativePos in NEAREST_RELATIVE_BLOCKS_FOR_RADIUS[radius]) {
            val block = relative(rootBlock, relativePos)
            val below = block.getRelative(BlockFace.DOWN)

            // The Block below must be farmland and the block itself must be air
            if (below.type == farmlandType && block.type == Material.AIR) {
                return block
            }
        }

        // We are outside the radius.
        return null
    }

    @JvmStatic
    fun updateLever(block: Block, face: BlockFace) {
        val level = (block.world as CraftWorld).handle
        val connectedBlock = block.getRelative(face.getOppositeFace())
        val blockPos1 = BlockPos(block.x, block.y, block.z)
        val blockPos2 = BlockPos(connectedBlock.x, connectedBlock.y, connectedBlock.z)
        val initiatorBlock = level.getBlockIfLoaded(blockPos1)
        level.updateNeighborsAt(blockPos1, initiatorBlock!!)
        level.updateNeighborsAt(blockPos2, initiatorBlock)
    }

    @JvmStatic
    fun raytraceOct(entity: LivingEntity, block: Block?): Oct? {
        // Ray-trace position and face
        val result = entity.rayTraceBlocks(10.0)
        if (block == null || result == null || (block != result.hitBlock)) {
            return null
        }

        // Get in-block hit position and bias the result
        // a bit inside the clicked face, so we don't get ambiguous results.
        val blockMiddle = block.location.toVector().add(Vector(0.5, 0.5, 0.5))
        val hit = result
            .hitPosition
            .subtract(blockMiddle)
            .subtract(result.hitBlockFace!!.getDirection().multiply(0.25))

        return Oct(hit, result.hitBlockFace)
    }

    @JvmStatic
    fun raytraceDominantFace(entity: LivingEntity, block: Block?): RaytraceDominantFaceResult? {
        // Ray trace clicked face
        val result = entity.rayTraceBlocks(10.0)
        if (block == null || result == null || (block != result.hitBlock)) {
            return null
        }

        val blockMiddle = block.location.toVector().add(Vector(0.5, 0.5, 0.5))
        val hitPosition = result.hitPosition
        val diff = hitPosition.subtract(blockMiddle)

        val ret = RaytraceDominantFaceResult()
        for (face in XZ_FACES) {
            // Calculate how dominant the current face contributes to the clicked point
            val faceDominance: Double = if (face.modX != 0) {
                diff.getX() * face.modX
            } else { // if (face.getModZ() != 0) {
                diff.getZ() * face.modZ
            }

            // Find maximum dominant face
            if (faceDominance > ret.dominance) {
                ret.face = face
                ret.dominance = faceDominance
            }
        }

        return ret
    }

    @JvmStatic
    fun textureFromSkull(skull: Skull): String? {
        val profile = skull.profile ?: return null

        for (property in profile.properties()) {
            if ("textures" == property.name) {
                return property.value
            }
        }

        return null
    }

    class Corner(private val x: Boolean, private val y: Boolean, private val z: Boolean) {
        private val id: Int

        constructor(hit: Vector) : this(hit.getX() >= 0.0, hit.getY() >= 0.0, hit.getZ() >= 0.0)

        init {
            val mx = if (x) 1 else 0
            val my = if (y) 1 else 0
            val mz = if (z) 1 else 0
            this.id = (mx shl 2) or (my shl 1) or (mz shl 0)
        }

        fun up(): Boolean {
            return y
        }

        fun east(): Boolean {
            return x
        }

        fun south(): Boolean {
            return z
        }

        fun up(up: Boolean): Corner {
            return Corner(x, up, z)
        }

        fun east(east: Boolean): Corner {
            return Corner(east, y, z)
        }

        fun south(south: Boolean): Corner {
            return Corner(x, y, south)
        }

        /** Rotates the corner as if it were on a north facing block given the block's rotation.  */
        fun rotateToNorthReference(rotation: BlockFace): Corner {
            return when (rotation) {
                BlockFace.NORTH -> Corner(x, y, z)
                BlockFace.EAST -> Corner(z, y, !x)
                BlockFace.SOUTH -> Corner(!x, y, !z)
                BlockFace.WEST -> Corner(!z, y, x)
                else -> throw IllegalArgumentException("rotation must be one of NORTH, EAST, SOUTH, WEST")
            }
        }

        /** Returns {NORTH, SOUTH}_{EAST, WEST} to indicate the XZ corner.  */
        fun xzFace(): BlockFace {
            return if (x) {
                if (z) {
                    BlockFace.SOUTH_EAST
                } else {
                    BlockFace.NORTH_EAST
                }
            } else {
                if (z) {
                    BlockFace.SOUTH_WEST
                } else {
                    BlockFace.NORTH_WEST
                }
            }
        }

        override fun hashCode(): Int {
            return id
        }

        override fun equals(other: Any?): Boolean {
            return other is Corner && other.id == id
        }
    }

    class Oct(// Relative to a block middle
        private val hitPos: Vector, private val face: BlockFace?
    ) {
        private val corner: Corner = Corner(hitPos)

        fun hitPos(): Vector {
            return hitPos
        }

        fun corner(): Corner {
            return corner
        }

        fun face(): BlockFace? {
            return face
        }
    }

    class RaytraceDominantFaceResult {
        @JvmField
        var face: BlockFace? = null
        @JvmField
        var dominance: Double = 0.0
    }

    class BlockVectorRadiusComparator : Comparator<BlockVector> {
        override fun compare(a: BlockVector, b: BlockVector): Int {
            return ((a.blockX * a.blockX + a.blockY * a.blockY + a.blockZ * a.blockZ) -
                    (b.blockX * b.blockX + b.blockY * b.blockY + b.blockZ * b.blockZ)
                    )
        }
    }
}
