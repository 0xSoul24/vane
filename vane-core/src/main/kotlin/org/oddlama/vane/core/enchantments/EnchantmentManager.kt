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
import java.util.function.Consumer

class EnchantmentManager(context: Context<Core?>?) : Listener<Core?>(context) {
    fun updateEnchantedItem(itemStack: ItemStack, onlyIfEnchanted: Boolean): ItemStack {
        return updateEnchantedItem(itemStack, HashMap(), onlyIfEnchanted)
    }

    @JvmOverloads
    fun updateEnchantedItem(
        itemStack: ItemStack,
        additionalEnchantments: MutableMap<Enchantment?, Int?>? = HashMap(),
        onlyIfEnchanted: Boolean = false
    ): ItemStack {
        removeOldLore(itemStack)
        return itemStack
    }

    private fun removeSuperseded(itemStack: ItemStack?, enchantments: MutableMap<Enchantment?, Int?>?) {
        // if (enchantments.isEmpty()) {
        // 	return;
        // }

        // 1. Build a list of all enchantments that would be removed because
        //    they are superseded by some enchantment.
        // final var to_remove_inclusive = enchantments.keySet().stream()
        // 	.map(x -> ((CraftEnchantment)x).getHandle())
        // 	.filter(x -> x instanceof NativeEnchantmentWrapper)
        // 	.map(x -> ((NativeEnchantmentWrapper)x).custom().supersedes())
        // 	.flatMap(Set::stream)
        // 	.collect(Collectors.toSet());

        // 2. Before removing these enchantments, first re-build the list but
        //    ignore any enchantments in the calculation that would themselves
        //    be removed. This prevents them from contributing to the list of
        //    enchantments to remove. Consider this: A supersedes B, and B supersedes C, but
        //    A doesn't supersede C. Now an item with A B and C should get reduced to
        //    A and C, not just to A.
        // var to_remove = enchantments.keySet().stream()
        // 	.map(x -> ((CraftEnchantment)x).getHandle())
        // 	.filter(x -> x instanceof NativeEnchantmentWrapper)
        // 	.filter(x ->
        // !to_remove_inclusive.contains(((NativeEnchantmentWrapper)x).custom().key())) // Ignore
        // enchantments that are themselves removed.
        // 	.map(x -> ((NativeEnchantmentWrapper)x).custom().supersedes())
        // 	.flatMap(Set::stream)
        // 	.map(x -> org.bukkit.Registry.ENCHANTMENT.get(x))
        // 	.collect(Collectors.toSet());

        // for (var e : to_remove) {
        // 	itemStack.removeEnchantment(e);
        // 	enchantments.remove(e);
        // }
    }

    private fun removeOldLore(itemStack: ItemStack) {
        var lore = itemStack.lore()
        if (lore == null) {
            lore = ArrayList<Component?>()
        }

        lore.removeIf { component: Component? -> this.isEnchantmentLore(component) }

        // Set lore
        itemStack.lore(if (lore.isEmpty()) null else lore)
    }

    private fun isEnchantmentLore(component: Component?): Boolean {
        // FIXME legacy If the component begins with a translated lore from vane enchantments, it is
        // always from us. (needed for backward compatibility)
        if (component is TranslatableComponent &&
            component.key().startsWith("vane_enchantments.")
        ) {
            return true
        }

        return ItemUtil.hasSentinel(component, SENTINEL)
    }

    // Triggers on Anvils, grindstones, and smithing tables.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPrepareEnchantedEdit(event: PrepareResultEvent) {
        if (event.result == null) {
            return
        }

        event.result = updateEnchantedItem(event.result!!.clone())
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEnchantItem(event: EnchantItemEvent) {
        val map = HashMap<Enchantment?, Int?>(event.enchantsToAdd)
        updateEnchantedItem(event.item, map)
    }

    // @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    // public void on_loot_generate(final LootGenerateEvent event) {
    // 	for (final var item : event.getLoot()) {
    // 		// Update all item lore in case they are enchanted
    // 		updateEnchantedItem(item, true);
    // 	}
    // }
    private fun processRecipe(recipe: MerchantRecipe): MerchantRecipe {
        val result = recipe.result.clone()

        // Create a new recipe
        val newRecipe = MerchantRecipe(
            updateEnchantedItem(result, true),
            recipe.uses,
            recipe.maxUses,
            recipe.hasExperienceReward(),
            recipe.villagerExperience,
            recipe.priceMultiplier
        )
        recipe.getIngredients().forEach(Consumer { i: ItemStack? -> newRecipe.addIngredient(i!!) })
        return newRecipe
    } // @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
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
