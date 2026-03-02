package org.oddlama.vane.enchantments.enchantments.registry

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys
import org.bukkit.enchantments.Enchantment
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry

class SeedingRegistry(composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>) :
    CustomEnchantmentRegistry("seeding", ItemTypeTagKeys.HOES, 4) {
    init {
        this.register(composeEvent)
    }
}
