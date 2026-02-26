package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.PlayerUtil.swingArm;

import java.util.List;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.StorageUtil;

@VaneItem(
    name = "lodestone_scroll",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 15,
    modelData = 0x760011,
    version = 1
)
public class LodestoneScroll extends Scroll {

    public static final NamespacedKey LODESTONE_LOCATION = StorageUtil.namespacedKey("vane", "lodestone_location");

    @LangMessage
    public TranslatedMessage langTeleportNoBoundLodestone;

    @LangMessage
    public TranslatedMessage langTeleportMissingLodestone;

    @LangMessage
    public TranslatedMessage langBoundLore;

    public LodestoneScroll(Context<Trifles> context) {
        super(context, 6000);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("ABA", "EPE")
                .setIngredient('P', "vane_trifles:papyrus_scroll")
                .setIngredient('E', Material.ENDER_PEARL)
                .setIngredient('A', Material.AMETHYST_SHARD)
                .setIngredient('B', Material.NETHERITE_INGOT)
                .result(key().toString())
        );
    }

    private Location getLodestoneLocation(final ItemStack scroll) {
        if (!scroll.hasItemMeta()) {
            return null;
        }
        return StorageUtil.storageGetLocation(
            scroll.getItemMeta().getPersistentDataContainer(),
            LODESTONE_LOCATION,
            null
        );
    }

    @Override
    public Location teleportLocation(final ItemStack scroll, Player player, boolean imminentTeleport) {
        // This scroll cannot be used while sneaking to allow re-binding
        if (player.isSneaking()) {
            return null;
        }

        final var lodestoneLocation = getLodestoneLocation(scroll);
        var lodestone = lodestoneLocation == null ? null : lodestoneLocation.getBlock();

        if (imminentTeleport) {
            if (lodestoneLocation == null) {
                langTeleportNoBoundLodestone.sendActionBar(player);
            } else if (lodestone.getType() != Material.LODESTONE) {
                langTeleportMissingLodestone.sendActionBar(player);
                lodestone = null;
            }
        }

        return lodestone == null ? null : lodestone.getLocation().add(0.5, 1.005, 0.5);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        // Skip if no block clicked or the item is allowed to be used (e.g., torches in offhand)
        if (
            !event.hasBlock() ||
            event.getAction() != Action.RIGHT_CLICK_BLOCK ||
            event.useItemInHand() == Event.Result.ALLOW
        ) {
            return;
        }

        final var block = event.getClickedBlock();
        if (block.getType() != Material.LODESTONE) {
            return;
        }

        // Only if player sneak-right-clicks
        final var player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        // With a lodestone scroll in the main hand
        final var item = player.getEquipment().getItem(EquipmentSlot.HAND);
        final var customItem = getModule().getCore().itemRegistry().get(item);
        if (!(customItem instanceof LodestoneScroll scroll) || !scroll.enabled()) {
            return;
        }

        // Save lodestone location
        item.editMeta(meta -> {
            StorageUtil.storageSetLocation(
                meta.getPersistentDataContainer(),
                LODESTONE_LOCATION,
                block.getLocation().add(0.5, 0.5, 0.5)
            );
            meta.lore(
                List.of(
                    langBoundLore
                        .format(
                            "§a" + block.getWorld().getName(),
                            "§b" + block.getX(),
                            "§b" + block.getY(),
                            "§b" + block.getZ()
                        )
                        .decoration(TextDecoration.ITALIC, false)
                )
            );
        });

        // Effects and sound
        swingArm(player, event.getHand());
        block
            .getWorld()
            .spawnParticle(Particle.ENCHANT, block.getLocation().add(0.5, 2.0, 0.5), 100, 0.1, 0.3, 0.1, 2.0);
        block
            .getWorld()
            .playSound(block.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.0f, 3.0f);

        // Prevent offhand from triggering (e.g., placing torches)
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }
}
