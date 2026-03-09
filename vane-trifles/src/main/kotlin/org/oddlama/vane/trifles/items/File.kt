package org.oddlama.vane.trifles.items

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.Directional
import org.bukkit.block.data.MultipleFacing
import org.bukkit.block.data.type.*
import org.bukkit.block.data.type.Tripwire
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.item.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.BlockUtil
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.PlayerUtil
import java.util.*

@VaneItem(name = "file", base = Material.WARPED_FUNGUS_ON_A_STICK, durability = 4000, modelData = 0x760003, version = 1)
class File(context: Context<Trifles?>) : CustomItem<Trifles?>(context) {
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape(" M", "S ")
                .setIngredient('M', Material.IRON_INGOT)
                .setIngredient('S', Material.STICK)
                .result(key().toString())
        )
    }

    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        return EnumSet.of(
            InhibitBehavior.USE_IN_VANILLA_RECIPE,
            InhibitBehavior.TEMPT,
            InhibitBehavior.USE_OFFHAND
        )
    }

    private fun nextFacing(allowedFaces: MutableSet<BlockFace?>, face: BlockFace?): BlockFace? {
        if (allowedFaces.isEmpty()) {
            return face
        }
        val list = ArrayList<BlockFace?>(allowedFaces)
        Collections.sort<BlockFace?>(list, Comparator { a: BlockFace?, b: BlockFace? -> a!!.ordinal - b!!.ordinal })
        val index = list.indexOf(face)
        if (index == -1) {
            return face
        }
        return list[(index + 1) % list.size]
    }

    private fun changeStairHalf(stairs: Stairs): Sound? {
        // Change half
        stairs.half = if (stairs.half == Bisected.Half.BOTTOM) Bisected.Half.TOP else Bisected.Half.BOTTOM
        return Sound.UI_STONECUTTER_TAKE_RESULT
    }

    private fun nextFaceCcw(face: BlockFace): BlockFace? {
        return when (face) {
            BlockFace.NORTH -> BlockFace.WEST
            BlockFace.EAST -> BlockFace.NORTH
            BlockFace.SOUTH -> BlockFace.EAST
            BlockFace.WEST -> BlockFace.SOUTH
            else -> null
        }
    }

    private fun changeStairShape(
        player: Player,
        block: Block?,
        stairs: Stairs,
        clickedFace: BlockFace?
    ): Sound? {
        // Check which eighth of the block was clicked
        val oct = BlockUtil.raytraceOct(player, block) ?: return null

        var corner = oct.corner()
        val isStairTop = stairs.half == Bisected.Half.TOP

        // If we clicked on the base part, toggle the corner upstate,
        // as we always want to reason abount the corner to add/remove.
        if (corner.up() == isStairTop) {
            corner = corner.up(!corner.up())
        }

        // Rotate a corner so that we can interpret it from
        // the reference facing (north)
        val originalFacing = stairs.facing
        corner = corner.rotateToNorthReference(originalFacing)

        // Determine the resulting shape, face and wether the oct got added or removed
        var shape: Stairs.Shape? = null
        var face: BlockFace? = null
        var added = false
        when (stairs.shape) {
            Stairs.Shape.STRAIGHT -> when (corner.xzFace()) {
                BlockFace.SOUTH_WEST -> {
                    shape = Stairs.Shape.INNER_LEFT
                    face = BlockFace.NORTH
                    added = true
                }

                BlockFace.NORTH_WEST -> {
                    shape = Stairs.Shape.OUTER_RIGHT
                    face = BlockFace.NORTH
                    added = false
                }

                BlockFace.NORTH_EAST -> {
                    shape = Stairs.Shape.OUTER_LEFT
                    face = BlockFace.NORTH
                    added = false
                }

                BlockFace.SOUTH_EAST -> {
                    shape = Stairs.Shape.INNER_RIGHT
                    face = BlockFace.NORTH
                    added = true
                }

                else -> {}
            }

            Stairs.Shape.INNER_LEFT -> when (corner.xzFace()) {
                BlockFace.SOUTH_WEST -> {
                    shape = Stairs.Shape.STRAIGHT
                    face = BlockFace.NORTH
                    added = false
                }

                BlockFace.NORTH_EAST -> {
                    shape = Stairs.Shape.STRAIGHT
                    face = BlockFace.WEST
                    added = false
                }

                else -> {}
            }

            Stairs.Shape.INNER_RIGHT -> when (corner.xzFace()) {
                BlockFace.NORTH_WEST -> {
                    shape = Stairs.Shape.STRAIGHT
                    face = BlockFace.EAST
                    added = false
                }

                BlockFace.SOUTH_EAST -> {
                    shape = Stairs.Shape.STRAIGHT
                    face = BlockFace.NORTH
                    added = false
                }

                else -> {}
            }

            Stairs.Shape.OUTER_LEFT -> when (corner.xzFace()) {
                BlockFace.SOUTH_WEST -> {
                    shape = Stairs.Shape.STRAIGHT
                    face = BlockFace.WEST
                    added = true
                }

                BlockFace.NORTH_EAST -> {
                    shape = Stairs.Shape.STRAIGHT
                    face = BlockFace.NORTH
                    added = true
                }

                else -> {}
            }

            Stairs.Shape.OUTER_RIGHT -> when (corner.xzFace()) {
                BlockFace.NORTH_WEST -> {
                    shape = Stairs.Shape.STRAIGHT
                    face = BlockFace.NORTH
                    added = true
                }

                BlockFace.SOUTH_EAST -> {
                    shape = Stairs.Shape.STRAIGHT
                    face = BlockFace.EAST
                    added = true
                }

                else -> {}
            }
        }

        // Break if the resulting shape is invalid
        if (shape == null) {
            return null
        }

        // Undo reference rotation
        when (face) {
            BlockFace.NORTH -> face = originalFacing
            BlockFace.EAST -> face = nextFaceCcw(originalFacing)!!.getOppositeFace()
            BlockFace.WEST -> face = nextFaceCcw(originalFacing)
            else -> {}
        }

        stairs.shape = shape
        stairs.facing = face!!

        return if (added) Sound.UI_STONECUTTER_TAKE_RESULT else Sound.BLOCK_GRINDSTONE_USE
    }

    private fun changeMultipleFacing(
        player: Player,
        block: Block?,
        mf: MultipleFacing?,
        clickedFace: BlockFace
    ): Sound? {
        var clickedFace = clickedFace
        val minFaces: Int
        if (mf is Fence || mf is GlassPane) {
            // Allow fences and glass panes to have 0 faces
            minFaces = 0

            // Trace which side is the dominant side
            val result = BlockUtil.raytraceDominantFace(player, block) ?: return null

            // Only replace facing choice if we did hit a side,
            // or if the dominance was big enough.
            if (result.dominance > .2 || (clickedFace != BlockFace.UP && clickedFace != BlockFace.DOWN)) {
                clickedFace = result.face!!
            }
        } else if (mf is Tripwire) {
            minFaces = 0
        } else {
            return null
        }

        // Check if the clicked face is allowed to change
        if (!mf.allowedFaces.contains(clickedFace)) {
            return null
        }

        val hasFace = mf.hasFace(clickedFace)
        if (hasFace && mf.faces.isEmpty()) {
            // Refuse to remove beyond minimum face count
            return null
        }

        // Toggle clicked block face
        mf.setFace(clickedFace, !hasFace)

        // Choose sound
        return if (hasFace) Sound.BLOCK_GRINDSTONE_USE else Sound.UI_STONECUTTER_TAKE_RESULT
    }

    private fun changeWall(player: Player, block: Block?, wall: Wall, clickedFace: BlockFace?): Sound? {
        // Trace which side is the dominant side
        val result = BlockUtil.raytraceDominantFace(player, block) ?: return null
        // Only replace facing choice if we did hit a side,
        // or if the dominance was big enough.
        val adjustedClickedFace = if (result.dominance > .2 || (clickedFace != BlockFace.UP && clickedFace != BlockFace.DOWN)) {
            result.face
        } else {
            clickedFace
        }

        if (clickedFace == BlockFace.UP) {
            val wasUp = wall.isUp
            if (adjustedClickedFace == BlockFace.UP) {
                // click top in middle -> toggle up
                wall.isUp = !wasUp
            } else if (BlockUtil.XZ_FACES.contains(adjustedClickedFace)) {
                // click top on side -> toggle height
                val height = wall.getHeight(adjustedClickedFace!!)
                when (height) {
                    Wall.Height.NONE -> return null
                    Wall.Height.LOW -> wall.setHeight(adjustedClickedFace, Wall.Height.TALL)
                    Wall.Height.TALL -> wall.setHeight(adjustedClickedFace, Wall.Height.LOW)
                }
            }

            return if (wasUp) Sound.BLOCK_GRINDSTONE_USE else Sound.UI_STONECUTTER_TAKE_RESULT
        } else {
            // click side -> toggle side
            val hasFace = wall.getHeight(adjustedClickedFace!!) != Wall.Height.NONE

            // Set height and choose sound
            if (hasFace) {
                wall.setHeight(adjustedClickedFace, Wall.Height.NONE)
                return Sound.BLOCK_GRINDSTONE_USE
            } else {
                // Use opposite face's height, or low if there is nothing.
                var targetHeight = wall.getHeight(adjustedClickedFace.getOppositeFace())
                if (targetHeight == Wall.Height.NONE) {
                    targetHeight = Wall.Height.LOW
                }
                wall.setHeight(adjustedClickedFace, targetHeight)
                return Sound.UI_STONECUTTER_TAKE_RESULT
            }
        }
    }

    private fun changeDirectionalFacing(
        player: Player?,
        block: Block?,
        directional: Directional,
        clickedFace: BlockFace?
    ): Sound? {
        // Toggle facing
        directional.facing = nextFacing(directional.faces, directional.facing)!!
        return Sound.UI_STONECUTTER_TAKE_RESULT
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        if (event.useItemInHand() == Event.Result.DENY) {
            return
        }

        // Get item variant
        val player = event.getPlayer()
        val item = player.equipment.getItem(event.hand!!)
        if (!isInstance(item)) {
            return
        }

        // Create a block break event for block to transmute and check if it gets canceled
        val block = event.clickedBlock
        val breakEvent = BlockBreakEvent(block!!, player)
        module!!.server.pluginManager.callEvent(breakEvent)
        if (breakEvent.isCancelled) {
            return
        }

        val data = block.blockData
        val clickedFace = event.blockFace

        val sound: Sound?
        if (player.isSneaking) {
            if (data is Stairs) {
                sound = changeStairHalf(data)
            } else {
                return
            }
        } else {
            sound = when (data) {
                is MultipleFacing -> {
                    changeMultipleFacing(player, block, data, clickedFace)
                }

                is Wall -> {
                    changeWall(player, block, data, clickedFace)
                }

                is Stairs -> {
                    changeStairShape(player, block, data, clickedFace)
                }

                else -> {
                    return
                }
            }
        }

        // Return if nothing was done
        if (sound == null) {
            return
        }

        // Update block data, and don't trigger physics! (We do not want to affect surrounding
        // blocks!)
        block.setBlockData(data, false)
        block.world.playSound(block.location, sound, SoundCategory.BLOCKS, 1.0f, 1.0f)

        // Damage item and swing arm
        ItemUtil.damageItem(player, item, 1)
        PlayerUtil.swingArm(player, event.hand!!)
    }
}
