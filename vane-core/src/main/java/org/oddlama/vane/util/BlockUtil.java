package org.oddlama.vane.util;

import static org.oddlama.vane.util.MaterialUtil.isReplaceableGrass;
import static org.oddlama.vane.util.MaterialUtil.isTillable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class BlockUtil {

    public static final List<BlockFace> BLOCK_FACES = Arrays.asList(
        BlockFace.NORTH,
        BlockFace.EAST,
        BlockFace.SOUTH,
        BlockFace.WEST,
        BlockFace.UP,
        BlockFace.DOWN
    );

    public static final List<BlockFace> XZ_FACES = Arrays.asList(
        BlockFace.NORTH,
        BlockFace.EAST,
        BlockFace.SOUTH,
        BlockFace.WEST
    );

    public static final int NEAREST_RELATIVE_BLOCKS_FOR_RADIUS_MAX = 6;
    public static final List<List<BlockVector>> NEAREST_RELATIVE_BLOCKS_FOR_RADIUS = new ArrayList<>();

    static {
        for (int i = 0; i <= NEAREST_RELATIVE_BLOCKS_FOR_RADIUS_MAX; ++i) {
            NEAREST_RELATIVE_BLOCKS_FOR_RADIUS.add(nearestBlocksForRadius(i));
        }
    }

    public static boolean equalsPos(final Block b1, final Block b2) {
        return b1.getX() == b2.getX() && b1.getY() == b2.getY() && b1.getZ() == b2.getZ();
    }

    public static void dropNaturally(Block block, ItemStack drop) {
        dropNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
    }

    public static void dropNaturally(Location loc, ItemStack drop) {
        loc
            .getWorld()
            .dropItem(loc.add(Vector.getRandom().subtract(new Vector(.5, .5, .5)).multiply(0.5)), drop)
            .setVelocity(Vector.getRandom().add(new Vector(-.5, +.5, -.5)).normalize().multiply(.15));
    }

    public static List<BlockVector> nearestBlocksForRadius(int radius) {
        final var ret = new ArrayList<BlockVector>();

        // Use square bounding box
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Only circular area
                if (x * x + z * z > radius * radius + 0.5) {
                    continue;
                }

                ret.add(new BlockVector(x, 0, z));
            }
        }

        Collections.sort(ret, new BlockVectorRadiusComparator());
        return ret;
    }

    public static @NotNull Block relative(@NotNull final Block block, @NotNull final Vector relative) {
        return block.getRelative(relative.getBlockX(), relative.getBlockY(), relative.getBlockZ());
    }

    public static Block nextTillableBlock(final Block rootBlock, int radius, boolean careless) {
        for (final var relativePos : NEAREST_RELATIVE_BLOCKS_FOR_RADIUS.get(radius)) {
            final var block = relative(rootBlock, relativePos);

            // Check for a tillable material
            if (!isTillable(block.getType())) {
                continue;
            }

            // Get block above
            final var above = block.getRelative(0, 1, 0);

            if (above.getType() == Material.AIR) {
                // If the block above is air, we can till the block.
                return block;
            } else if (careless && isReplaceableGrass(above.getType())) {
                // If the block above is replaceable grass, delete it and return the block.
                above.setType(Material.AIR);
                return block;
            }
        }

        // We are outside the radius.
        return null;
    }

    public static Block[] adjacentBlocks3D(final Block root) {
        final var adjacent = new Block[26];

        // Direct adjacent
        adjacent[0] = root.getRelative(1, 0, 0);
        adjacent[1] = root.getRelative(-1, 0, 0);
        adjacent[2] = root.getRelative(0, 0, 1);
        adjacent[3] = root.getRelative(0, 0, -1);
        adjacent[4] = root.getRelative(0, 1, 0);
        adjacent[5] = root.getRelative(0, -1, 0);

        // Edge adjacent
        adjacent[6] = root.getRelative(0, -1, -1);
        adjacent[7] = root.getRelative(0, -1, 1);
        adjacent[8] = root.getRelative(0, 1, -1);
        adjacent[9] = root.getRelative(0, 1, 1);
        adjacent[10] = root.getRelative(-1, 0, -1);
        adjacent[11] = root.getRelative(-1, 0, 1);
        adjacent[12] = root.getRelative(1, 0, -1);
        adjacent[13] = root.getRelative(1, 0, 1);
        adjacent[14] = root.getRelative(-1, -1, 0);
        adjacent[15] = root.getRelative(-1, 1, 0);
        adjacent[16] = root.getRelative(1, -1, 0);
        adjacent[17] = root.getRelative(1, 1, 0);

        // Corner adjacent
        adjacent[18] = root.getRelative(-1, -1, -1);
        adjacent[19] = root.getRelative(-1, -1, 1);
        adjacent[20] = root.getRelative(-1, 1, -1);
        adjacent[21] = root.getRelative(-1, 1, 1);
        adjacent[22] = root.getRelative(1, -1, -1);
        adjacent[23] = root.getRelative(1, -1, 1);
        adjacent[24] = root.getRelative(1, 1, -1);
        adjacent[25] = root.getRelative(1, 1, 1);

        return adjacent;
    }

    public static Block nextSeedableBlock(final Block rootBlock, Material farmlandType, int radius) {
        for (var relativePos : NEAREST_RELATIVE_BLOCKS_FOR_RADIUS.get(radius)) {
            final var block = relative(rootBlock, relativePos);
            final var below = block.getRelative(BlockFace.DOWN);

            // The Block below must be farmland and the block itself must be air
            if (below.getType() == farmlandType && block.getType() == Material.AIR) {
                return block;
            }
        }

        // We are outside the radius.
        return null;
    }

    public static void updateLever(final Block block, final BlockFace face) {
        final var level = ((CraftWorld) block.getWorld()).getHandle();
        final var connectedBlock = block.getRelative(face.getOppositeFace());
        final var blockPos1 = new BlockPos(block.getX(), block.getY(), block.getZ());
        final var blockPos2 = new BlockPos(connectedBlock.getX(), connectedBlock.getY(), connectedBlock.getZ());
        final var initiatorBlock = level.getBlockIfLoaded(blockPos1);
        level.updateNeighborsAt(blockPos1, initiatorBlock);
        level.updateNeighborsAt(blockPos2, initiatorBlock);
    }

    public static class Corner {

        private boolean x;
        private boolean y;
        private boolean z;
        private int id;

        public Corner(final Vector hit) {
            this(hit.getX() >= 0.0, hit.getY() >= 0.0, hit.getZ() >= 0.0);
        }

        public Corner(boolean x, boolean y, boolean z) {
            this.x = x;
            this.y = y;
            this.z = z;
            final var mx = x ? 1 : 0;
            final var my = y ? 1 : 0;
            final var mz = z ? 1 : 0;
            this.id = (mx << 2) | (my << 1) | (mz << 0);
        }

        public boolean up() {
            return y;
        }

        public boolean east() {
            return x;
        }

        public boolean south() {
            return z;
        }

        public Corner up(boolean up) {
            return new Corner(x, up, z);
        }

        public Corner east(boolean east) {
            return new Corner(east, y, z);
        }

        public Corner south(boolean south) {
            return new Corner(x, y, south);
        }

        /** Rotates the corner as if it were on a north facing block given the block's rotation. */
        public Corner rotateToNorthReference(final BlockFace rotation) {
            switch (rotation) {
                default:
                    throw new IllegalArgumentException("rotation must be one of NORTH, EAST, SOUTH, WEST");
                case NORTH:
                    return new Corner(x, y, z);
                case EAST:
                    return new Corner(z, y, !x);
                case SOUTH:
                    return new Corner(!x, y, !z);
                case WEST:
                    return new Corner(!z, y, x);
            }
        }

        /** Returns {NORTH, SOUTH}_{EAST, WEST} to indicate the XZ corner. */
        public BlockFace xzFace() {
            if (x) {
                if (z) {
                    return BlockFace.SOUTH_EAST;
                } else {
                    return BlockFace.NORTH_EAST;
                }
            } else {
                if (z) {
                    return BlockFace.SOUTH_WEST;
                } else {
                    return BlockFace.NORTH_WEST;
                }
            }
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Corner && ((Corner) other).id == id;
        }
    }

    public static class Oct {

        private Vector hitPos; // Relative to a block middle
        private Corner corner;
        private BlockFace face;

        public Oct(final Vector hitPos, final BlockFace face) {
            this.hitPos = hitPos;
            this.corner = new Corner(hitPos);
            this.face = face;
        }

        public Vector hitPos() {
            return hitPos;
        }

        public Corner corner() {
            return corner;
        }

        public BlockFace face() {
            return face;
        }
    }

    public static Oct raytraceOct(final LivingEntity entity, final Block block) {
        // Ray-trace position and face
        final var result = entity.rayTraceBlocks(10.0);
        if (block == null || result == null || !block.equals(result.getHitBlock())) {
            return null;
        }

        // Get in-block hit position and bias the result
        // a bit inside the clicked face, so we don't get ambiguous results.
        final var blockMiddle = block.getLocation().toVector().add(new Vector(0.5, 0.5, 0.5));
        final var hit = result
            .getHitPosition()
            .subtract(blockMiddle)
            .subtract(result.getHitBlockFace().getDirection().multiply(0.25));

        return new Oct(hit, result.getHitBlockFace());
    }

    public static class RaytraceDominantFaceResult {

        public BlockFace face = null;
        public double dominance = 0.0;
    }

    public static RaytraceDominantFaceResult raytraceDominantFace(final LivingEntity entity, final Block block) {
        // Ray trace clicked face
        final var result = entity.rayTraceBlocks(10.0);
        if (block == null || result == null || !block.equals(result.getHitBlock())) {
            return null;
        }

        final var blockMiddle = block.getLocation().toVector().add(new Vector(0.5, 0.5, 0.5));
        final var hitPosition = result.getHitPosition();
        final var diff = hitPosition.subtract(blockMiddle);

        final var ret = new RaytraceDominantFaceResult();
        for (final var face : BlockUtil.XZ_FACES) {
            // Calculate how dominant the current face contributes to the clicked point
            final double faceDominance;
            if (face.getModX() != 0) {
                faceDominance = diff.getX() * face.getModX();
            } else { // if (face.getModZ() != 0) {
                faceDominance = diff.getZ() * face.getModZ();
            }

            // Find maximum dominant face
            if (faceDominance > ret.dominance) {
                ret.face = face;
                ret.dominance = faceDominance;
            }
        }

        return ret;
    }

    public static String textureFromSkull(final Skull skull) {
        final var profile = skull.getProfile();
        if (profile == null) {
            return null;
        }

        for (final var property : profile.properties()) {
            if ("textures".equals(property.getName())) {
                return property.getValue();
            }
        }

        return null;
    }

    public static class BlockVectorRadiusComparator implements Comparator<BlockVector> {

        @Override
        public int compare(BlockVector a, BlockVector b) {
            return (
                (a.getBlockX() * a.getBlockX() + a.getBlockY() * a.getBlockY() + a.getBlockZ() * a.getBlockZ()) -
                (b.getBlockX() * b.getBlockX() + b.getBlockY() * b.getBlockY() + b.getBlockZ() * b.getBlockZ())
            );
        }
    }
}
