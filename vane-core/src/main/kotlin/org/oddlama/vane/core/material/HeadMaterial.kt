package org.oddlama.vane.core.material

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.json.JSONObject
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.StorageUtil.namespacedKey

class HeadMaterial(
    val key: NamespacedKey?,
    val name: String?,
    val category: String?,
    tags: List<String?>,
    val texture: String?
) {
    val tags: Set<String?> = tags.toHashSet()

    fun item(): ItemStack = ItemUtil.skullWithTexture(name!!, texture!!)

    companion object {
        @JvmStatic
        fun from(json: JSONObject): HeadMaterial {
            val id = json.getString("id")
            val name = json.getString("name")
            val category = json.getString("category")
            val texture = json.getString("texture")
            val tagsArr = json.getJSONArray("tags")
            val tags = (0 until tagsArr.length()).map { tagsArr.getString(it) }
            val key = namespacedKey("vane", "${category}_$id")
            return HeadMaterial(key, name, category, tags, texture)
        }
    }
}
