package org.oddlama.vane.core.material

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.util.MaterialUtil.materialFrom

class ExtendedMaterial private constructor(val key: NamespacedKey) {
    private val material: Material? = materialFrom(key)
    private val headMaterial: HeadMaterial? = if (material == null) HeadMaterialLibrary.from(key) else null

    val isSimpleMaterial: Boolean get() = material != null

    @JvmOverloads
    fun item(amount: Int = 1): ItemStack? {
        headMaterial?.let { return it.item().also { i -> i.amount = amount } }
        material?.let { return ItemStack(it, amount) }

        val customItem = Core.instance()?.itemRegistry()?.get(key)
        checkNotNull(customItem) { "ExtendedMaterial '$key' is neither a classic material, a head nor a custom item!" }
        return customItem.newStack()
    }

    companion object {
        @JvmStatic
        fun from(key: NamespacedKey): ExtendedMaterial? {
            val mat = ExtendedMaterial(key)
            return if (mat.material == null && mat.headMaterial == null && key.namespace() == "minecraft") null
                   else mat
        }

        @JvmStatic
        fun from(material: Material): ExtendedMaterial? = from(material.key)

        @JvmStatic
        fun from(customItem: CustomItem): ExtendedMaterial? = from(customItem.key())
    }
}
