package org.oddlama.vane.enchantments

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemType
import java.util.function.Consumer

abstract class CustomEnchantmentRegistry {
    var key: Key
    var description: Component
    var maxLevel: Int

    var supportedItemTags: TagKey<ItemType>? = null
    var supportedItems: MutableList<TypedKey<ItemType>> = mutableListOf()

    var exclusiveWithTags: TagKey<Enchantment>? = null
    var exclusiveWith: MutableList<TypedKey<Enchantment>> = mutableListOf()

    constructor(name: String, supportedItemTags: TagKey<ItemType>, maxLevel: Int) {
        this.key = Key.key(NAMESPACE, name)
        val pascal: String = snakeCaseToPascalCase(name)
        this.description = Component.translatable("$NAMESPACE.Enchantment$pascal.Name")
        this.supportedItemTags = supportedItemTags
        this.maxLevel = maxLevel
    }

    constructor(name: String, supportedItems: MutableList<TypedKey<ItemType>>, maxLevel: Int) {
        this.key = Key.key(NAMESPACE, name)
        val pascal: String = snakeCaseToPascalCase(name)
        this.description = Component.translatable("$NAMESPACE.Enchantment$pascal.Name")
        this.supportedItems = supportedItems
        this.maxLevel = maxLevel
    }

    /**
     * Add exclusive enchantments to this enchantment: exclusive enchantments can't be on the same
     * tool.
     */
    fun exclusiveWith(enchantments: MutableList<TypedKey<Enchantment>>): CustomEnchantmentRegistry {
        this.exclusiveWith = enchantments
        return this
    }

    /**
     * Add exclusive enchantment **tag** to this enchantment: exclusive enchantments can't be on
     * the same tool.
     */
    fun exclusiveWith(enchantmentTag: TagKey<Enchantment>?): CustomEnchantmentRegistry {
        this.exclusiveWithTags = enchantmentTag
        return this
    }

    /** Get exclusive enchantments  */
    fun exclusiveWith(
        composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>
    ): RegistryKeySet<Enchantment> {
        return if (this.exclusiveWithTags != null) {
            composeEvent.getOrCreateTag(exclusiveWithTags!!)
        } else {
            RegistrySet.keySet(RegistryKey.ENCHANTMENT, this.exclusiveWith)
        }
    }

    /**
     * Register the enchantment in the registry
     * 
     * @see [Paper Registry Documentation](https://docs.papermc.io/paper/dev/registries.create-new-entries)
     */
    fun register(composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>) {
        composeEvent
            .registry()
            .register(
                TypedKey.create(RegistryKey.ENCHANTMENT, key),
                Consumer { e: EnchantmentRegistryEntry.Builder? ->
                    e!!
                        .description(description)
                        .supportedItems(
                            if (supportedItems.isNotEmpty())
                                RegistrySet.keySet(RegistryKey.ITEM, supportedItems)
                            else
                                composeEvent.getOrCreateTag(supportedItemTags!!)
                        )
                        .anvilCost(1)
                        .maxLevel(maxLevel)
                        .weight(10)
                        .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 1))
                        .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(3, 1))
                        .activeSlots(EquipmentSlotGroup.ANY)
                        .exclusiveWith(this.exclusiveWith(composeEvent))
                }
            )
    }

    fun typedKey(name: String): TypedKey<Enchantment> {
        return TypedKey.create(RegistryKey.ENCHANTMENT, Key.key(NAMESPACE, name))
    }

    companion object {
        const val NAMESPACE: String = "vane_enchantments"

        // Utility: convert snake_case names like "lightning" or "life_mending" to PascalCase "Lightning"/"LifeMending"
        private fun snakeCaseToPascalCase(snake: String): String {
            val parts: Array<String?> = snake.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val sb = StringBuilder()
            for (part in parts) {
                if (part.isNullOrEmpty()) continue
                sb.append(part[0].uppercaseChar())
                if (part.length > 1) sb.append(part.substring(1))
            }
            return sb.toString()
        }
    }
}
