package org.oddlama.vane.enchantments.enchantments.registry

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.keys.EnchantmentKeys
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys
import org.bukkit.enchantments.Enchantment
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry

class UnbreakableRegistry(composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>) :
    CustomEnchantmentRegistry("unbreakable", ItemTypeTagKeys.ENCHANTABLE_DURABILITY, 1) {
    init {
        this.exclusiveWith(mutableListOf(EnchantmentKeys.UNBREAKING, EnchantmentKeys.MENDING))
        this.register(composeEvent)
    }
}
