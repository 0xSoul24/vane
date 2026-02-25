package org.oddlama.vane.core.material

import org.bukkit.NamespacedKey
import org.json.JSONArray
import org.oddlama.vane.core.material.HeadMaterial.Companion.from

object HeadMaterialLibrary {
    private val registry: MutableMap<NamespacedKey?, HeadMaterial?> = HashMap()
    private val categories: MutableMap<String?, MutableList<HeadMaterial?>> =
        HashMap()
    private val tags: MutableMap<String?, MutableList<HeadMaterial?>> = HashMap()
    private val byTexture: MutableMap<String?, HeadMaterial?> = HashMap()

    @JvmStatic
    fun load(string: String) {
        val json = JSONArray(string)
        for (i in 0..<json.length()) {
            // Deserialize
            val mat = from(json.getJSONObject(i))

            // Add to registry
            registry[mat.key()] = mat
            byTexture[mat.texture()] = mat

            // Add to category lookup
            val category = categories.computeIfAbsent(mat.category()) { k: String? -> ArrayList() }
            category.add(mat)

            // Add to tag lookup
            for (tag in mat.tags()) {
                val tagList = tags.computeIfAbsent(tag) { k: String? -> ArrayList() }
                tagList.add(mat)
            }
        }
    }

    @JvmStatic
    fun from(key: NamespacedKey?): HeadMaterial? {
        return registry[key]
    }

    @JvmStatic
    fun fromTexture(base64Texture: String?): HeadMaterial? {
        return byTexture[base64Texture]
    }

    @JvmStatic
    fun all(): MutableCollection<HeadMaterial?> {
        return registry.values
    }
}
