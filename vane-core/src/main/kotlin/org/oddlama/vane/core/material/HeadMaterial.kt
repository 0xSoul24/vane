package org.oddlama.vane.core.material

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.json.JSONObject
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.StorageUtil.namespacedKey

/**
 * Data model for a predefined custom player-head material entry.
 *
 * @param key namespaced key identifier.
 * @param name display name.
 * @param category category label.
 * @param tags tag list.
 * @param texture base64 texture payload.
 */
class HeadMaterial(
    /** Unique namespaced identifier. */
    val key: NamespacedKey?,
    /** Display name of this head entry. */
    val name: String?,
    /** Category label grouping this head. */
    val category: String?,
    tags: List<String?>,
    /** Base64 texture payload. */
    val texture: String?
) {
    /** Unique tag set for this head material. */
    val tags: Set<String?> = tags.toHashSet()

    /** Creates an item stack representing this head material. */
    fun item(): ItemStack = ItemUtil.skullWithTexture(name!!, texture!!)

    /** JSON deserialization helpers. */
    companion object {
        @JvmStatic
        /** Creates a [HeadMaterial] from a serialized JSON object. */
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
