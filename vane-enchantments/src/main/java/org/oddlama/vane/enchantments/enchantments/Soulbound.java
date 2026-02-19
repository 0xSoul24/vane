package org.oddlama.vane.enchantments.enchantments;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.loot.LootDefinition;
import org.oddlama.vane.core.config.loot.LootTableList;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.data.CooldownData;
import org.oddlama.vane.core.enchantments.CustomEnchantment;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.util.StorageUtil;

@VaneEnchantment(name = "soulbound", rarity = Rarity.RARE, treasure = true, allowCustom = true)
public class Soulbound extends CustomEnchantment<Enchantments> {

    @ConfigLong(
        def = 2000,
        min = 0,
        desc = "Window to allow Soulbound item drop immediately after a previous drop in milliseconds"
    )
    public long configCooldown;

    private static final NamespacedKey IGNORE_SOULBOUND_DROP = StorageUtil.namespacedKey(
        "vane_enchantments",
        "ignore_soulbound_drop"
    );
    private CooldownData dropCooldown = new CooldownData(IGNORE_SOULBOUND_DROP, configCooldown);

    @LangMessage
    public TranslatedMessage langDropLockWarning;

    @LangMessage
    public TranslatedMessage langDroppedNotification;

    @LangMessage
    public TranslatedMessage langDropCooldown;

    public Soulbound(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public void onConfigChange() {
        super.onConfigChange();
        dropCooldown = new CooldownData(IGNORE_SOULBOUND_DROP, configCooldown);
    }

    @Override
    public RecipeList defaultRecipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("CQC", "OBE", "RGT")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_the_gods")
                .setIngredient('C', Material.IRON_CHAIN)
                .setIngredient('Q', Material.WRITABLE_BOOK)
                .setIngredient('O', Material.BONE)
                .setIngredient('R', "minecraft:enchanted_book#enchants{minecraft:binding_curse*1}")
                .setIngredient('G', Material.GHAST_TEAR)
                .setIngredient('T', Material.TOTEM_OF_UNDYING)
                .setIngredient('E', Material.ENDER_EYE)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        );
    }

    @Override
    public LootTableList defaultLootTables() {
        return LootTableList.of(
            new LootDefinition("generic")
                .in(LootTables.BASTION_TREASURE)
                .add(1.0 / 15, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        );
    }

    @Override
    public Component applyDisplayFormat(Component component) {
        return component.color(NamedTextColor.DARK_GRAY);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final var keepItems = event.getItemsToKeep();

        // Keep all soulbound items
        final var it = event.getDrops().iterator();
        while (it.hasNext()) {
            final var drop = it.next();
            if (isSoulbound(drop)) {
                keepItems.add(drop);
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInventoryCheck(final InventoryClickEvent event) {
        if (event.getCursor() == null) return;
        if (!isSoulbound(event.getCursor())) return;
        if (
            event.getAction() == InventoryAction.DROP_ALL_CURSOR || event.getAction() == InventoryAction.DROP_ONE_CURSOR
        ) {
            boolean tooSlow = dropCooldown.peekCooldown(event.getCursor().getItemMeta());
            if (tooSlow) {
                // Dropped too slowly, refresh and cancel
                final ItemMeta meta = event.getCursor().getItemMeta();
                dropCooldown.checkOrUpdateCooldown(meta);
                event.getCursor().setItemMeta(meta);
                langDropCooldown.sendActionBar(event.getWhoClicked());
                event.setResult(Event.Result.DENY);
                return;
            }
            // else allow as normal
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        // A player cannot drop soulbound items.
        // Prevents yeeting your best sword out of existence.
        // (It's okay to put them into chests.)
        final var droppedItem = event.getItemDrop().getItemStack();
        if (isSoulbound(droppedItem)) {
            boolean tooSlow = dropCooldown.peekCooldown(droppedItem.getItemMeta());
            if (!tooSlow) {
                var meta = droppedItem.getItemMeta();
                dropCooldown.clear(droppedItem.getItemMeta());
                droppedItem.setItemMeta(meta);
                langDroppedNotification.send(event.getPlayer(), droppedItem.displayName());
                return;
            }
            final var inventory = event.getPlayer().getInventory();
            if (inventory.firstEmpty() != -1) {
                // We still have space in the inventory, so the player tried to drop it with Q.
                event.setCancelled(true);
                langDropLockWarning.sendActionBar(
                    event.getPlayer(),
                    event.getItemDrop().getItemStack().displayName()
                );
            } else {
                // Inventory is full (e.g., when exiting crafting table with soulbound item in it)
                // so we drop the first non-soulbound item (if any) instead.
                final var it = inventory.iterator();
                ItemStack nonSoulboundItem = null;
                int nonSoulboundItemSlot = 0;
                while (it.hasNext()) {
                    final var item = it.next();
                    if (item.getEnchantmentLevel(this.bukkit()) == 0) {
                        nonSoulboundItem = item;
                        break;
                    }

                    ++nonSoulboundItemSlot;
                }

                if (nonSoulboundItem == null) {
                    // We can't prevent dropping a soulbound item.
                    // Well, that sucks.
                    return;
                }

                // Drop the other item
                final var player = event.getPlayer();
                inventory.setItem(nonSoulboundItemSlot, droppedItem);
                player.getLocation().getWorld().dropItem(player.getLocation(), nonSoulboundItem);
                langDropLockWarning.sendActionBar(player, event.getItemDrop().getItemStack().displayName());
                event.setCancelled(true);
            }
        }
    }

    private boolean isSoulbound(ItemStack droppedItem) {
        return droppedItem.getEnchantmentLevel(this.bukkit()) > 0;
    }
}
