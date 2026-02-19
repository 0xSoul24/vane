package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.BlockUtil.raytraceDominantFace;
import static org.oddlama.vane.util.BlockUtil.raytraceOct;
import static org.oddlama.vane.util.ItemUtil.damageItem;
import static org.oddlama.vane.util.PlayerUtil.swingArm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.BlockUtil;

@VaneItem(
    name = "file",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 4000,
    modelData = 0x760003,
    version = 1
)
public class File extends CustomItem<Trifles> {

    public File(Context<Trifles> context) {
        super(context);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape(" M", "S ")
                .setIngredient('M', Material.IRON_INGOT)
                .setIngredient('S', Material.STICK)
                .result(key().toString())
        );
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.TEMPT, InhibitBehavior.USE_OFFHAND);
    }

    private BlockFace nextFacing(Set<BlockFace> allowedFaces, BlockFace face) {
        if (allowedFaces.isEmpty()) {
            return face;
        }
        final var list = new ArrayList<BlockFace>(allowedFaces);
        Collections.sort(list, (a, b) -> a.ordinal() - b.ordinal());
        final var index = list.indexOf(face);
        if (index == -1) {
            return face;
        }
        return list.get((index + 1) % list.size());
    }

    private Sound changeStairHalf(final Stairs stairs) {
        // Change half
        stairs.setHalf(stairs.getHalf() == Bisected.Half.BOTTOM ? Bisected.Half.TOP : Bisected.Half.BOTTOM);
        return Sound.UI_STONECUTTER_TAKE_RESULT;
    }

    private BlockFace nextFaceCcw(final BlockFace face) {
        switch (face) {
            default:
                return null;
            case NORTH:
                return BlockFace.WEST;
            case EAST:
                return BlockFace.NORTH;
            case SOUTH:
                return BlockFace.EAST;
            case WEST:
                return BlockFace.SOUTH;
        }
    }

    private Sound changeStairShape(
        final Player player,
        final Block block,
        final Stairs stairs,
        final BlockFace clickedFace
    ) {
        // Check which eighth of the block was clicked
        final var oct = raytraceOct(player, block);
        if (oct == null) {
            return null;
        }

        var corner = oct.corner();
        final var isStairTop = stairs.getHalf() == Bisected.Half.TOP;

        // If we clicked on the base part, toggle the corner upstate,
        // as we always want to reason abount the corner to add/remove.
        if (corner.up() == isStairTop) {
            corner = corner.up(!corner.up());
        }

        // Rotate a corner so that we can interpret it from
        // the reference facing (north)
        final var originalFacing = stairs.getFacing();
        corner = corner.rotateToNorthReference(originalFacing);

        // Determine the resulting shape, face and wether the oct got added or removed
        Stairs.Shape shape = null;
        BlockFace face = null;
        boolean added = false;
        switch (stairs.getShape()) {
            case STRAIGHT:
                switch (corner.xzFace()) {
                    case SOUTH_WEST:
                        shape = Stairs.Shape.INNER_LEFT;
                        face = BlockFace.NORTH;
                        added = true;
                        break;
                    case NORTH_WEST:
                        shape = Stairs.Shape.OUTER_RIGHT;
                        face = BlockFace.NORTH;
                        added = false;
                        break;
                    case NORTH_EAST:
                        shape = Stairs.Shape.OUTER_LEFT;
                        face = BlockFace.NORTH;
                        added = false;
                        break;
                    case SOUTH_EAST:
                        shape = Stairs.Shape.INNER_RIGHT;
                        face = BlockFace.NORTH;
                        added = true;
                        break;
                    default:
                        break;
                }
                break;
            case INNER_LEFT:
                switch (corner.xzFace()) {
                    case SOUTH_WEST:
                        shape = Stairs.Shape.STRAIGHT;
                        face = BlockFace.NORTH;
                        added = false;
                        break;
                    case NORTH_EAST:
                        shape = Stairs.Shape.STRAIGHT;
                        face = BlockFace.WEST;
                        added = false;
                        break;
                    default:
                        break;
                }
                break;
            case INNER_RIGHT:
                switch (corner.xzFace()) {
                    case NORTH_WEST:
                        shape = Stairs.Shape.STRAIGHT;
                        face = BlockFace.EAST;
                        added = false;
                        break;
                    case SOUTH_EAST:
                        shape = Stairs.Shape.STRAIGHT;
                        face = BlockFace.NORTH;
                        added = false;
                        break;
                    default:
                        break;
                }
                break;
            case OUTER_LEFT:
                switch (corner.xzFace()) {
                    case SOUTH_WEST:
                        shape = Stairs.Shape.STRAIGHT;
                        face = BlockFace.WEST;
                        added = true;
                        break;
                    case NORTH_EAST:
                        shape = Stairs.Shape.STRAIGHT;
                        face = BlockFace.NORTH;
                        added = true;
                        break;
                    default:
                        break;
                }
                break;
            case OUTER_RIGHT:
                switch (corner.xzFace()) {
                    case NORTH_WEST:
                        shape = Stairs.Shape.STRAIGHT;
                        face = BlockFace.NORTH;
                        added = true;
                        break;
                    case SOUTH_EAST:
                        shape = Stairs.Shape.STRAIGHT;
                        face = BlockFace.EAST;
                        added = true;
                        break;
                    default:
                        break;
                }
                break;
        }

        // Break if the resulting shape is invalid
        if (shape == null) {
            return null;
        }

        // Undo reference rotation
        switch (face) {
            case NORTH:
                face = originalFacing;
                break;
            case EAST:
                face = nextFaceCcw(originalFacing).getOppositeFace();
                break;
            case SOUTH:
                face = originalFacing.getOppositeFace();
                break;
            case WEST:
                face = nextFaceCcw(originalFacing);
                break;
            default:
                break;
        }

        stairs.setShape(shape);
        stairs.setFacing(face);

        return added ? Sound.UI_STONECUTTER_TAKE_RESULT : Sound.BLOCK_GRINDSTONE_USE;
    }

    private Sound changeMultipleFacing(
        final Player player,
        final Block block,
        final MultipleFacing mf,
        BlockFace clickedFace
    ) {
        final int minFaces;
        if (mf instanceof Fence || mf instanceof GlassPane) {
            // Allow fences and glass panes to have 0 faces
            minFaces = 0;

            // Trace which side is the dominant side
            final var result = raytraceDominantFace(player, block);
            if (result == null) {
                return null;
            }

            // Only replace facing choice if we did hit a side,
            // or if the dominance was big enough.
            if (result.dominance > .2 || (clickedFace != BlockFace.UP && clickedFace != BlockFace.DOWN)) {
                clickedFace = result.face;
            }
        } else if (mf instanceof Tripwire) {
            minFaces = 0;
        } else {
            return null;
        }

        // Check if the clicked face is allowed to change
        if (!mf.getAllowedFaces().contains(clickedFace)) {
            return null;
        }

        boolean hasFace = mf.hasFace(clickedFace);
        if (hasFace && minFaces >= mf.getFaces().size()) {
            // Refuse to remove beyond minimum face count
            return null;
        }

        // Toggle clicked block face
        mf.setFace(clickedFace, !hasFace);

        // Choose sound
        return hasFace ? Sound.BLOCK_GRINDSTONE_USE : Sound.UI_STONECUTTER_TAKE_RESULT;
    }

    private Sound changeWall(final Player player, final Block block, final Wall wall, final BlockFace clickedFace) {
        // Trace which side is the dominant side
        final var result = raytraceDominantFace(player, block);
        if (result == null) {
            return null;
        }

        final BlockFace adjustedClickedFace;
        // Only replace facing choice if we did hit a side,
        // or if the dominance was big enough.
        if (result.dominance > .2 || (clickedFace != BlockFace.UP && clickedFace != BlockFace.DOWN)) {
            adjustedClickedFace = result.face;
        } else {
            adjustedClickedFace = clickedFace;
        }

        if (clickedFace == BlockFace.UP) {
            final var wasUp = wall.isUp();
            if (adjustedClickedFace == BlockFace.UP) {
                // click top in middle -> toggle up
                wall.setUp(!wasUp);
            } else if (BlockUtil.XZ_FACES.contains(adjustedClickedFace)) {
                // click top on side -> toggle height
                final var height = wall.getHeight(adjustedClickedFace);
                switch (height) {
                    case NONE:
                        return null;
                    case LOW:
                        wall.setHeight(adjustedClickedFace, Wall.Height.TALL);
                        break;
                    case TALL:
                        wall.setHeight(adjustedClickedFace, Wall.Height.LOW);
                        break;
                }
            }

            return wasUp ? Sound.BLOCK_GRINDSTONE_USE : Sound.UI_STONECUTTER_TAKE_RESULT;
        } else {
            // click side -> toggle side
            final var hasFace = wall.getHeight(adjustedClickedFace) != Wall.Height.NONE;

            // Set height and choose sound
            if (hasFace) {
                wall.setHeight(adjustedClickedFace, Wall.Height.NONE);
                return Sound.BLOCK_GRINDSTONE_USE;
            } else {
                // Use opposite face's height, or low if there is nothing.
                var targetHeight = wall.getHeight(adjustedClickedFace.getOppositeFace());
                if (targetHeight == Wall.Height.NONE) {
                    targetHeight = Wall.Height.LOW;
                }
                wall.setHeight(adjustedClickedFace, targetHeight);
                return Sound.UI_STONECUTTER_TAKE_RESULT;
            }
        }
    }

    private Sound changeDirectionalFacing(
        final Player player,
        final Block block,
        final Directional directional,
        final BlockFace clickedFace
    ) {
        // Toggle facing
        directional.setFacing(nextFacing(directional.getFaces(), directional.getFacing()));
        return Sound.UI_STONECUTTER_TAKE_RESULT;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerRightClick(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        // Get item variant
        final var player = event.getPlayer();
        final var item = player.getEquipment().getItem(event.getHand());
        if (!isInstance(item)) {
            return;
        }

        // Create a block break event for block to transmute and check if it gets canceled
        final var block = event.getClickedBlock();
        final var breakEvent = new BlockBreakEvent(block, player);
        getModule().getServer().getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) {
            return;
        }

        final var data = block.getBlockData();
        final var clickedFace = event.getBlockFace();

        final Sound sound;
        if (player.isSneaking()) {
            if (data instanceof Stairs) {
                sound = changeStairHalf((Stairs) data);
            } else {
                return;
            }
        } else {
            if (data instanceof MultipleFacing) {
                sound = changeMultipleFacing(player, block, (MultipleFacing) data, clickedFace);
            } else if (data instanceof Wall) {
                sound = changeWall(player, block, (Wall) data, clickedFace);
            } else if (data instanceof Stairs) {
                sound = changeStairShape(player, block, (Stairs) data, clickedFace);
            } else {
                return;
            }
        }

        // Return if nothing was done
        if (sound == null) {
            return;
        }

        // Update block data, and don't trigger physics! (We do not want to affect surrounding
        // blocks!)
        block.setBlockData(data, false);
        block.getWorld().playSound(block.getLocation(), sound, SoundCategory.BLOCKS, 1.0f, 1.0f);

        // Damage item and swing arm
        damageItem(player, item, 1);
        swingArm(player, event.getHand());
    }
}
