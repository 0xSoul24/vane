package org.oddlama.vane.trifles.items

import net.kyori.adventure.text.Component
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.*
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.annotation.lang.LangMessageArray
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.lang.TranslatedMessageArray
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.Nms
import org.oddlama.vane.util.PlayerUtil
import org.oddlama.vane.util.StorageUtil
import java.util.*
import java.util.function.Consumer

@VaneItem(
    name = "trowel",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 800,
    modelData = 0x76000e,
    version = 1
)
class Trowel(context: Context<Trifles?>) : org.oddlama.vane.core.item.CustomItem<Trifles?>(context) {
    enum class FeedSource(private val displayName: String, private val slots: IntArray) {
        HOTBAR("Hotbar", intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)),
        FIRST_ROW("First Inventory Row", intArrayOf(9, 10, 11, 12, 13, 14, 15, 16, 17)),
        SECOND_ROW("Second Inventory Row", intArrayOf(18, 19, 20, 21, 22, 23, 24, 25, 26)),
        THIRD_ROW("Third Inventory Row", intArrayOf(27, 28, 29, 30, 31, 32, 33, 34, 35));

        fun displayName(): String {
            return displayName
        }

        fun next(): FeedSource {
            return entries[(this.ordinal + 1) % entries.size]
        }

        fun slots(): IntArray {
            return slots
        }
    }

    @LangMessageArray
    var langLore: TranslatedMessageArray? = null

    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("  S", "MM ")
                .setIngredient('M', Material.IRON_INGOT)
                .setIngredient('S', Material.STICK)
                .result(key().toString())
        )
    }

    private fun feedSource(itemStack: ItemStack): FeedSource {
        val meta = itemStack.itemMeta ?: return FeedSource.HOTBAR
        val ord = meta.persistentDataContainer.getOrDefault(FEED_SOURCE, PersistentDataType.INTEGER, 0)
        if (ord < 0 || ord >= FeedSource.entries.size) {
            return FeedSource.HOTBAR
        }
        return FeedSource.entries[ord]
    }

    private fun feedSource(itemStack: ItemStack, feedSource: FeedSource) {
        itemStack.editMeta(Consumer { meta: ItemMeta? ->
            meta!!.persistentDataContainer.set(FEED_SOURCE, PersistentDataType.INTEGER, feedSource.ordinal)
        })
    }

    private fun updateLore(itemStack: ItemStack) {
        var lore = itemStack.lore()
        if (lore == null) {
            lore = ArrayList()
        }

        // Remove old lore, add updated lore
        lore.removeIf { component: Component? -> isTrowelLore(component) }

        val feedSource = feedSource(itemStack)
        lore.addAll(
            langLore!!.format("§a$feedSource").stream()
                .map { x: Component? -> ItemUtil.addSentinel(x!!, SENTINEL) }.toList()
        )

        itemStack.lore(lore)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerClickInventory(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) {
            return
        }

        // Only on right-click item, when nothing is on the cursor
        if (event.action != InventoryAction.PICKUP_HALF ||
            (event.cursor.type != Material.AIR)
        ) {
            return
        }

        val item = event.getCurrentItem()
        val customItem: CustomItem? = module!!.core?.itemRegistry()?.get(item)
        if (customItem !is Trowel || !customItem.enabled()) {
            return
        }

        // Use next feed source
        val feedSource = feedSource(item!!)
        feedSource(item, feedSource.next())
        updateLore(item)

        val player = event.whoClicked as Player
        player.playSound(player, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 5.0f)
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteractBlock(event: PlayerInteractEvent) {
        // Skip if no block was right-clicked or hand isn't main hand
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND
        ) {
            return
        }

        // With a trowel in main hand
        val player = event.getPlayer()
        val itemInHand = player.equipment.getItem(EquipmentSlot.HAND)
        val customItem: CustomItem? = module!!.core?.itemRegistry()?.get(itemInHand)
        if (customItem !is Trowel || !customItem.enabled()) {
            return
        }

        // Prevent offhand from triggering (e.g., placing torches)
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)

        // Select a random block from the feed source and place it
        val block = event.clickedBlock
        val inventory = player.inventory
        val fedSource = feedSource(itemInHand)
        val possibleSlots = fedSource.slots().clone()
        var count = possibleSlots.size
        while (count > 0) {
            val index: Int = random.nextInt(count)
            val itemStack = inventory.getItem(possibleSlots[index])
            // Skip empty slots and items that are not placeable blocks
            if (itemStack == null || !itemStack.type.isBlock ||
                Tag.SHULKER_BOXES.isTagged(itemStack.type)
            ) {
                // Eliminate the end of list, so copy item at the end of list to the index (<
                // count).
                possibleSlots[index] = possibleSlots[--count]
                continue
            }
            val customItemSlot: CustomItem? = module!!.core?.itemRegistry()
                ?.get(itemStack)
            // if the item is a custom item, don't place it
            if (customItemSlot != null) {
                possibleSlots[index] = possibleSlots[--count]
                continue
            }

            val nmsItem = Nms.itemHandle(itemStack)
            val nmsPlayer = Nms.playerHandle(player)
            val nmsWorld = Nms.worldHandle(player.world)

            // Prepare context to place the item via NMS
            val direction = CraftBlock.blockFaceToNotch(event.blockFace)
            val blockPos = BlockPos(block!!.x, block.y, block.z)
            val interactionPoint = event.getInteractionPoint()
            val hitPos = Vec3(interactionPoint!!.x, interactionPoint.y, interactionPoint.z)
            val blockHitResult = BlockHitResult(hitPos, direction, blockPos, false)
            val amountPre = nmsItem!!.count
            val actionContext = UseOnContext(
                nmsWorld,
                nmsPlayer,
                InteractionHand.MAIN_HAND,
                nmsItem,
                blockHitResult
            )

            // Get sound now, otherwise the itemstack might be consumed afterward
            var soundType: SoundType? = null
            if (nmsItem.item is BlockItem) {
                val blockItem = nmsItem.item as BlockItem
                val placeState: BlockState? = blockItem
                    .block
                    .getStateForPlacement(BlockPlaceContext(actionContext))
                soundType = placeState!!.soundType
            }

            // Place the item by calling NMS to get correct placing behavior
            val result = nmsItem.useOn(actionContext)

            // Don't consume item in creative mode
            if (player.gameMode == GameMode.CREATIVE) {
                nmsItem.count = amountPre
            }

            if (result.consumesAction()) {
                PlayerUtil.swingArm(player, EquipmentSlot.HAND)
                ItemUtil.damageItem(player, itemInHand, 1)
                if (soundType != null) {
                    nmsWorld.playSound(
                        null,
                        blockPos,
                        soundType.placeSound,
                        SoundSource.BLOCKS,
                        (soundType.getVolume() + 1.0f) / 2.0f,
                        soundType.getPitch() * 0.8f
                    )
                }
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(nmsPlayer!!, blockPos, nmsItem)
            }

            nmsPlayer!!.connection.send(ClientboundBlockUpdatePacket(nmsWorld, blockPos))
            nmsPlayer.connection.send(ClientboundBlockUpdatePacket(nmsWorld, blockPos.relative(direction)))
            return
        }

        // No item found in any possible slot.
        player.playSound(player, Sound.UI_STONECUTTER_SELECT_RECIPE, SoundCategory.MASTER, 1.0f, 2.0f)
    }

    override fun updateItemStack(itemStack: ItemStack): ItemStack {
        updateLore(itemStack)
        return itemStack
    }

    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.USE_OFFHAND)
    }

    companion object {
        private val SENTINEL = StorageUtil.namespacedKey("vane", "trowel_lore")
        val FEED_SOURCE: NamespacedKey = StorageUtil.namespacedKey("vane", "feed_source")
        private val random = Random(23584982345L)

        /** Returns true if the given component is associated to the trowel.  */
        private fun isTrowelLore(component: Component?): Boolean {
            return ItemUtil.hasSentinel(component, SENTINEL)
        }
    }
}
