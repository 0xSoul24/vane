package org.oddlama.vane.core.material

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.util.MaterialUtil.materialFrom

class ExtendedMaterial private constructor(private val key: NamespacedKey) {
    private var material: Material? = materialFrom(key)
    private var headMaterial: HeadMaterial? = null

    init {
        if (this.material == null) {
            this.headMaterial = HeadMaterialLibrary.from(key)
        } else {
            this.headMaterial = null
        }
    }

    fun key(): NamespacedKey {
        return key
    }

    val isSimpleMaterial: Boolean
        get() = material != null

    @JvmOverloads
    fun item(amount: Int = 1): ItemStack? {
        if (headMaterial != null) {
            val item = headMaterial!!.item()
            item.amount = amount
            return item
        }
        if (material != null) {
            return ItemStack(material!!, amount)
        }

        val customItem = Core.instance()?.itemRegistry()?.get(key)
        checkNotNull(customItem) { "ExtendedMaterial '$key' is neither a classic material, a head nor a custom item!" }

        return customItem.newStack()
    }

    companion object {
        @JvmStatic
        fun from(key: NamespacedKey): ExtendedMaterial? {
            val mat = ExtendedMaterial(key)
            if (mat.material == null && mat.headMaterial == null && key.namespace() == "minecraft") {
                // If no material was found and the key doesn't suggest a custom item, return null.
                return null
            }
            return mat
        }

        @JvmStatic
        fun from(material: Material): ExtendedMaterial? {
            return from(material.getKey())
        }

        @JvmStatic
        fun from(customItem: CustomItem): ExtendedMaterial? {
            return from(customItem.key()!!)
        }
    }
}
