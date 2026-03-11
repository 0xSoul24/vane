package org.oddlama.vane.core.enchantments

import com.destroystokyo.paper.event.inventory.PrepareResultEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.StorageUtil.namespacedKey

class EnchantmentManager(context: Context<Core?>?) : Listener<Core?>(context) {
    fun updateEnchantedItem(itemStack: ItemStack, onlyIfEnchanted: Boolean): ItemStack =
        updateEnchantedItem(itemStack, HashMap(), onlyIfEnchanted)

    @JvmOverloads
    fun updateEnchantedItem(
        itemStack: ItemStack,
        additionalEnchantments: MutableMap<Enchantment?, Int?>? = HashMap(),
        onlyIfEnchanted: Boolean = false
    ): ItemStack {
        removeOldLore(itemStack)
        return itemStack
    }

    private fun removeOldLore(itemStack: ItemStack) {
        val lore = itemStack.lore() ?: return
        lore.removeIf { isEnchantmentLore(it) }
        itemStack.lore(lore.ifEmpty { null })
    }

    private fun isEnchantmentLore(component: Component?): Boolean {
        if (component is TranslatableComponent && component.key().startsWith("vane_enchantments.")) {
            return true
        }
        return ItemUtil.hasSentinel(component, SENTINEL)
    }

    // Triggers on Anvils, grindstones, and smithing tables.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPrepareEnchantedEdit(event: PrepareResultEvent) {
        val result = event.result ?: return
        event.result = updateEnchantedItem(result.clone())
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEnchantItem(event: EnchantItemEvent) {
        updateEnchantedItem(event.item, HashMap(event.enchantsToAdd))
    }

    // @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    // public void on_loot_generate(final LootGenerateEvent event) {
    // 	for (final var item : event.getLoot()) {
    // 		// Update all item lore in case they are enchanted
    // 		updateEnchantedItem(item, true);
    // 	}
    // }
    private fun processRecipe(recipe: MerchantRecipe): MerchantRecipe =
        MerchantRecipe(
            updateEnchantedItem(recipe.result.clone(), true),
            recipe.uses, recipe.maxUses,
            recipe.hasExperienceReward(),
            recipe.villagerExperience,
            recipe.priceMultiplier
        ).also { newRecipe -> recipe.ingredients.forEach { newRecipe.addIngredient(it) } }

    // @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    // public void on_acquire_trade(final VillagerAcquireTradeEvent event) {
    // 	event.setRecipe(processRecipe(event.getRecipe()));
    // }
    // @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    // public void on_right_click_villager(final PlayerInteractEntityEvent event) {
    // 	final var entity = event.getRightClicked();
    // 	if (!(entity instanceof Merchant)) {
    // 		return;
    // 	}
    // 	final var merchant = (Merchant) entity;
    // 	final var recipes = new ArrayList<MerchantRecipe>();
    // 	// Check all recipes
    // 	for (final var r : merchant.getRecipes()) {
    // 		recipes.add(processRecipe(r));
    // 	}
    // 	// Update recipes
    // 	merchant.setRecipes(recipes);
    // }

    companion object {
        private val SENTINEL = namespacedKey("vane", "enchantment_lore")
    }
}
