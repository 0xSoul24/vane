package org.oddlama.vane.enchantments.enchantments.registry

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.keys.ItemTypeKeys
import org.bukkit.enchantments.Enchantment
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry

class TakeOffRegistry(composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>) :
    CustomEnchantmentRegistry("take_off", mutableListOf(ItemTypeKeys.ELYTRA), 3) {
    init {
        this.register(composeEvent)
    }
}
