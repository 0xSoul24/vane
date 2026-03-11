package org.oddlama.vane.core.material

import org.bukkit.NamespacedKey
import org.json.JSONArray
import org.oddlama.vane.core.material.HeadMaterial.Companion.from

object HeadMaterialLibrary {
    private val registry   = mutableMapOf<NamespacedKey, HeadMaterial>()
    private val categories = mutableMapOf<String, MutableList<HeadMaterial>>()
    private val tags       = mutableMapOf<String, MutableList<HeadMaterial>>()
    private val byTexture  = mutableMapOf<String, HeadMaterial>()

    @JvmStatic
    fun load(string: String) {
        val json = JSONArray(string)
        for (i in 0 until json.length()) {
            // Deserialize
            val mat = from(json.getJSONObject(i))

            // Add to registry
            registry[mat.key!!] = mat
            byTexture[mat.texture!!] = mat

            // Add to category lookup
            categories.getOrPut(mat.category!!) { mutableListOf() }.add(mat)

            // Add to tag lookup
            mat.tags.forEach { tag ->
                tags.getOrPut(tag!!) { mutableListOf() }.add(mat)
            }
        }
    }

    @JvmStatic
    fun from(key: NamespacedKey?): HeadMaterial? = registry[key]

    @JvmStatic
    fun fromTexture(base64Texture: String?): HeadMaterial? = byTexture[base64Texture]

    @JvmStatic
    fun all(): Collection<HeadMaterial> = registry.values
}
