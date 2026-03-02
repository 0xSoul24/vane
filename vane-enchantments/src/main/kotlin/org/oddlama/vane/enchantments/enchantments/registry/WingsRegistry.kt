package org.oddlama.vane.enchantments.enchantments.registry

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.keys.ItemTypeKeys
import org.bukkit.enchantments.Enchantment
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry

class WingsRegistry(composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>) :
    CustomEnchantmentRegistry("wings", mutableListOf(ItemTypeKeys.ELYTRA), 4) {
    init {
        this.exclusiveWith(mutableListOf(typedKey("wings")))
        this.register(composeEvent)
    }
}
