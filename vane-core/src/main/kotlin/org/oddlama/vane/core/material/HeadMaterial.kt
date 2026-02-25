package org.oddlama.vane.core.material

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.json.JSONObject
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.StorageUtil.namespacedKey

class HeadMaterial(
    private val key: NamespacedKey?,
    private val name: String?,
    private val category: String?,
    tags: MutableList<String?>,
    private val base64Texture: String?
) {
    private val tags: MutableSet<String?> = HashSet(tags)

    fun key(): NamespacedKey? {
        return key
    }

    fun name(): String? {
        return name
    }

    fun category(): String? {
        return category
    }

    fun tags(): MutableSet<String?> {
        return tags
    }

    fun texture(): String? {
        return base64Texture
    }

    fun item(): ItemStack {
        return ItemUtil.skullWithTexture(name!!, base64Texture!!)
    }

    companion object {
        @JvmStatic
        fun from(json: JSONObject): HeadMaterial {
            val id = json.getString("id")
            val name = json.getString("name")
            val category = json.getString("category")
            val texture = json.getString("texture")

            val tags = ArrayList<String?>()
            val tagsArr = json.getJSONArray("tags")
            for (i in 0..<tagsArr.length()) {
                tags.add(tagsArr.getString(i))
            }

            val key = namespacedKey("vane", category + "_" + id)
            return HeadMaterial(key, name, category, tags, texture)
        }
    }
}
