package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.ItemUtil.damageItem;
import static org.oddlama.vane.util.PlayerUtil.swingArm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.annotation.lang.LangMessageArray;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.lang.TranslatedMessageArray;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.Nms;
import org.oddlama.vane.util.StorageUtil;

@VaneItem(
    name = "trowel",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 800,
    modelData = 0x76000e,
    version = 1
)
public class Trowel extends CustomItem<Trifles> {

    private static final NamespacedKey SENTINEL = StorageUtil.namespacedKey("vane", "trowel_lore");
    public static final NamespacedKey FEED_SOURCE = StorageUtil.namespacedKey("vane", "feed_source");
    private static Random random = new Random(23584982345l);

    public enum FeedSource {
        HOTBAR("Hotbar", new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 }),
        FIRST_ROW("First Inventory Row", new int[] { 9, 10, 11, 12, 13, 14, 15, 16, 17 }),
        SECOND_ROW("Second Inventory Row", new int[] { 18, 19, 20, 21, 22, 23, 24, 25, 26 }),
        THIRD_ROW("Third Inventory Row", new int[] { 27, 28, 29, 30, 31, 32, 33, 34, 35 });

        private String displayName;
        private int[] slots;

        private FeedSource(final String displayName, int[] slots) {
            this.displayName = displayName;
            this.slots = slots;
        }

        public String displayName() {
            return displayName;
        }

        public FeedSource next() {
            return FeedSource.values()[(this.ordinal() + 1) % FeedSource.values().length];
        }

        public int[] slots() {
            return slots;
        }
    }

    @LangMessageArray
    public TranslatedMessageArray langLore;

    public Trowel(final Context<Trifles> context) {
        super(context);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("  S", "MM ")
                .setIngredient('M', Material.IRON_INGOT)
                .setIngredient('S', Material.STICK)
                .result(key().toString())
        );
    }

    /** Returns true if the given component is associated to the trowel. */
    private static boolean isTrowelLore(final Component component) {
        return ItemUtil.hasSentinel(component, SENTINEL);
    }

    private FeedSource feedSource(final ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) {
            return FeedSource.HOTBAR;
        }
        final var ord = itemStack
            .getItemMeta()
            .getPersistentDataContainer()
            .getOrDefault(FEED_SOURCE, PersistentDataType.INTEGER, 0);
        if (ord < 0 || ord > FeedSource.values().length) {
            return FeedSource.HOTBAR;
        }
        return FeedSource.values()[ord];
    }

    private void feedSource(final ItemStack itemStack, final FeedSource feedSource) {
        itemStack.editMeta(meta ->
            meta.getPersistentDataContainer().set(FEED_SOURCE, PersistentDataType.INTEGER, feedSource.ordinal())
        );
    }

    private void updateLore(final ItemStack itemStack) {
        var lore = itemStack.lore();
        if (lore == null) {
            lore = new ArrayList<Component>();
        }

        // Remove old lore, add updated lore
        lore.removeIf(Trowel::isTrowelLore);

        final var feedSource = feedSource(itemStack);
        lore.addAll(
            langLore.format("§a" + feedSource).stream().map(x -> ItemUtil.addSentinel(x, SENTINEL)).toList()
        );

        itemStack.lore(lore);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerClickInventory(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Only on right-click item, when nothing is on the cursor
        if (
            event.getAction() != InventoryAction.PICKUP_HALF ||
            (event.getCursor() != null && event.getCursor().getType() != Material.AIR)
        ) {
            return;
        }

        final var item = event.getCurrentItem();
        final var customItem = getModule().getCore().itemRegistry().get(item);
        if (!(customItem instanceof Trowel trowel) || !trowel.enabled()) {
            return;
        }

        // Use next feed source
        final var feedSource = feedSource(item);
        feedSource(item, feedSource.next());
        updateLore(item);

        player.playSound(player, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 5.0f);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractBlock(final PlayerInteractEvent event) {
        // Skip if no block was right-clicked or hand isn't main hand
        if (
            !event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
        ) {
            return;
        }

        // With a trowel in main hand
        final var player = event.getPlayer();
        final var itemInHand = player.getEquipment().getItem(EquipmentSlot.HAND);
        final var customItem = getModule().getCore().itemRegistry().get(itemInHand);
        if (!(customItem instanceof Trowel trowel) || !trowel.enabled()) {
            return;
        }

        // Prevent offhand from triggering (e.g., placing torches)
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        // Select a random block from the feed source and place it
        final var block = event.getClickedBlock();
        final var inventory = player.getInventory();
        final var fedSource = feedSource(itemInHand);
        final var possibleSlots = fedSource.slots().clone();
        int count = possibleSlots.length;
        while (count > 0) {
            final var index = random.nextInt(count);
            final var itemStack = inventory.getItem(possibleSlots[index]);
            // Skip empty slots and items that are not placeable blocks
            if (
                itemStack == null ||
                !itemStack.getType().isBlock() ||
                Tag.SHULKER_BOXES.isTagged(itemStack.getType())
            ) {
                // Eliminate the end of list, so copy item at the end of list to the index (<
                // count).
                possibleSlots[index] = possibleSlots[--count];
                continue;
            }
            org.oddlama.vane.core.item.api.CustomItem customItemSlot = getModule().getCore().itemRegistry()
                .get(itemStack);
            // if the item is a custom item, don't place it
            if (customItemSlot != null) {
                possibleSlots[index] = possibleSlots[--count];
                continue;
            }

            final var nmsItem = Nms.itemHandle(itemStack);
            final var nmsPlayer = Nms.playerHandle(player);
            final var nmsWorld = Nms.worldHandle(player.getWorld());

            // Prepare context to place the item via NMS
            final var direction = CraftBlock.blockFaceToNotch(event.getBlockFace());
            final var blockPos = new BlockPos(block.getX(), block.getY(), block.getZ());
            final var interactionPoint = event.getInteractionPoint();
            final var hitPos = new Vec3(interactionPoint.getX(), interactionPoint.getY(), interactionPoint.getZ());
            final var blockHitResult = new BlockHitResult(hitPos, direction, blockPos, false);
            final var amountPre = nmsItem.getCount();
            final var actionContext = new UseOnContext(
                nmsWorld,
                nmsPlayer,
                InteractionHand.MAIN_HAND,
                nmsItem,
                blockHitResult
            );

            // Get sound now, otherwise the itemstack might be consumed afterwards
            SoundType soundType = null;
            if (nmsItem.getItem() instanceof BlockItem blockItem) {
                final var placeState = blockItem
                    .getBlock()
                    .getStateForPlacement(new BlockPlaceContext(actionContext));
                soundType = placeState.getSoundType();
            }

            // Place the item by calling NMS to get correct placing behavior
            final var result = nmsItem.useOn(actionContext);

            // Don't consume item in creative mode
            if (player.getGameMode() == GameMode.CREATIVE) {
                nmsItem.setCount(amountPre);
            }

            if (result.consumesAction()) {
                swingArm(player, EquipmentSlot.HAND);
                damageItem(player, itemInHand, 1);
                if (soundType != null) {
                    nmsWorld.playSound(
                        null,
                        blockPos,
                        soundType.getPlaceSound(),
                        SoundSource.BLOCKS,
                        (soundType.getVolume() + 1.0F) / 2.0F,
                        soundType.getPitch() * 0.8F
                    );
                }
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(nmsPlayer, blockPos, nmsItem);
            }

            nmsPlayer.connection.send(new ClientboundBlockUpdatePacket(nmsWorld, blockPos));
            nmsPlayer.connection.send(new ClientboundBlockUpdatePacket(nmsWorld, blockPos.relative(direction)));
            return;
        }

        // No item found in any possible slot.
        player.playSound(player, Sound.UI_STONECUTTER_SELECT_RECIPE, SoundCategory.MASTER, 1.0f, 2.0f);
    }

    @Override
    public ItemStack updateItemStack(final ItemStack itemStack) {
        updateLore(itemStack);
        return itemStack;
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.USE_OFFHAND);
    }
}
