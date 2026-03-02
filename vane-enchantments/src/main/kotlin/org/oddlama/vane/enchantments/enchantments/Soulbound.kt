package org.oddlama.vane.enchantments.enchantments

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootTables
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.loot.LootDefinition
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.data.CooldownData
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments
import org.oddlama.vane.util.StorageUtil

@VaneEnchantment(name = "soulbound", rarity = Rarity.RARE, treasure = true, allowCustom = true)
class Soulbound(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    @ConfigLong(
        def = 2000,
        min = 0,
        desc = "Window to allow Soulbound item drop immediately after a previous drop in milliseconds"
    )
    var configCooldown: Long = 0

    private var dropCooldown = CooldownData(IGNORE_SOULBOUND_DROP, configCooldown)

    @LangMessage
    var langDropLockWarning: TranslatedMessage? = null

    @LangMessage
    var langDroppedNotification: TranslatedMessage? = null

    @LangMessage
    var langDropCooldown: TranslatedMessage? = null

    public override fun onConfigChange() {
        super.onConfigChange()
        dropCooldown = CooldownData(IGNORE_SOULBOUND_DROP, configCooldown)
    }

    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
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
        )
    }

    override fun defaultLootTables(): LootTableList {
        return LootTableList.of(
            LootDefinition("generic")
                .`in`(LootTables.BASTION_TREASURE)
                .add(1.0 / 15, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    override fun applyDisplayFormat(component: Component): Component {
        return component.color(NamedTextColor.DARK_GRAY)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val keepItems = event.itemsToKeep

        // Keep all soulbound items
        val it = event.drops.iterator()
        while (it.hasNext()) {
            val drop = it.next()
            if (isSoulbound(drop)) {
                keepItems.add(drop)
                it.remove()
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInventoryCheck(event: InventoryClickEvent) {
        if (!isSoulbound(event.cursor)) return
        if (event.action == InventoryAction.DROP_ALL_CURSOR || event.action == InventoryAction.DROP_ONE_CURSOR
        ) {
            val tooSlow = dropCooldown.peekCooldown(event.cursor.itemMeta)
            if (tooSlow) {
                // Dropped too slowly, refresh and cancel
                val meta = event.cursor.itemMeta
                dropCooldown.checkOrUpdateCooldown(meta)
                event.cursor.setItemMeta(meta)
                langDropCooldown!!.sendActionBar(event.whoClicked)
                event.result = Event.Result.DENY
                return
            }
            // else allow as normal
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        // A player cannot drop soulbound items.
        // Prevents yeeting your best sword out of existence.
        // (It's okay to put them into chests.)
        val droppedItem = event.itemDrop.itemStack
        if (isSoulbound(droppedItem)) {
            val tooSlow = dropCooldown.peekCooldown(droppedItem.itemMeta)
            if (!tooSlow) {
                val meta = droppedItem.itemMeta
                dropCooldown.clear(droppedItem.itemMeta)
                droppedItem.setItemMeta(meta)
                langDroppedNotification!!.send(event.getPlayer(), droppedItem.displayName())
                return
            }
            val inventory = event.getPlayer().inventory
            if (inventory.firstEmpty() != -1) {
                // We still have space in the inventory, so the player tried to drop it with Q.
                event.isCancelled = true
                langDropLockWarning!!.sendActionBar(
                    event.getPlayer(),
                    event.itemDrop.itemStack.displayName()
                )
            } else {
                // Inventory is full (e.g., when exiting crafting table with soulbound item in it)
                // so we drop the first non-soulbound item (if any) instead.
                val it = inventory.iterator()
                var nonSoulboundItem: ItemStack? = null
                var nonSoulboundItemSlot = 0
                while (it.hasNext()) {
                    val item = it.next()
                    if (item.getEnchantmentLevel(this.bukkit()!!) == 0) {
                        nonSoulboundItem = item
                        break
                    }

                    ++nonSoulboundItemSlot
                }

                if (nonSoulboundItem == null) {
                    // We can't prevent dropping a soulbound item.
                    // Well, that sucks.
                    return
                }

                // Drop the other item
                val player = event.getPlayer()
                inventory.setItem(nonSoulboundItemSlot, droppedItem)
                player.location.getWorld().dropItem(player.location, nonSoulboundItem)
                langDropLockWarning!!.sendActionBar(player, event.itemDrop.itemStack.displayName())
                event.isCancelled = true
            }
        }
    }

    private fun isSoulbound(droppedItem: ItemStack): Boolean {
        return droppedItem.getEnchantmentLevel(this.bukkit()!!) > 0
    }

    companion object {
        private val IGNORE_SOULBOUND_DROP = StorageUtil.namespacedKey(
            "vane_enchantments",
            "ignore_soulbound_drop"
        )
    }
}
