package org.oddlama.vane.core.item

import org.bukkit.NamespacedKey
import org.oddlama.vane.core.item.api.CustomModelDataRegistry

class CustomModelDataRegistry : CustomModelDataRegistry {
    private val reservedRanges = HashMap<NamespacedKey, CustomModelDataRegistry.Range>()

    override fun has(data: Int): Boolean {
        return reservedRanges.values.any { it.contains(data) }
    }

    override fun hasAny(range: CustomModelDataRegistry.Range?): Boolean {
        if (range == null) return false
        return reservedRanges.values.any { it.overlaps(range) }
    }

    override fun get(resourceKey: NamespacedKey?): CustomModelDataRegistry.Range? {
        return reservedRanges[resourceKey]
    }

    override fun get(data: Int): NamespacedKey? {
        return reservedRanges.entries.firstOrNull { it.value.contains(data) }?.key
    }

    override fun get(range: CustomModelDataRegistry.Range?): NamespacedKey? {
        if (range == null) return null
        return reservedRanges.entries.firstOrNull { it.value.overlaps(range) }?.key
    }

    override fun reserve(resourceKey: NamespacedKey?, range: CustomModelDataRegistry.Range?) {
        if (resourceKey == null || range == null) return
        val existing = get(range)
        require(existing == null) { "Cannot reserve range $range, already registered by $existing" }
        reservedRanges[resourceKey] = range
    }

    override fun reserveSingle(resourceKey: NamespacedKey?, data: Int) {
        if (resourceKey == null) return
        val existing = get(data)
        require(existing == null) { "Cannot reserve customModelData $data, already registered by $existing" }
        reservedRanges[resourceKey] = CustomModelDataRegistry.Range(data, data + 1)
    }
}
