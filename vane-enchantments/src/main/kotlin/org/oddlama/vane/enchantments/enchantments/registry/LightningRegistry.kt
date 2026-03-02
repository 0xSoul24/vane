package org.oddlama.vane.enchantments.enchantments.registry

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys
import org.bukkit.enchantments.Enchantment
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry

class LightningRegistry(composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>) :
    CustomEnchantmentRegistry("lightning", ItemTypeTagKeys.SWORDS, 1) {
    init {
        this.register(composeEvent)
    }
}
