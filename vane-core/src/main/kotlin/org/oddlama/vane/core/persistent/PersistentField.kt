package org.oddlama.vane.core.persistent

import org.json.JSONObject
import java.io.IOException
import java.lang.reflect.Field

class PersistentField(private val owner: Any?, private val field: Field, mapName: (String?) -> String?) {
    private val path: String? = mapName(field.name.substring("storage".length))

    init {
        field.isAccessible = true
    }

    fun path(): String? = path

    fun get(): Any? =
        try {
            field.get(owner)
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }

    @Throws(IOException::class)
    fun save(json: JSONObject) {
        json.put(path, PersistentSerializer.toJson(field, get()) ?: JSONObject.NULL)
    }

    @Throws(IOException::class)
    fun load(json: JSONObject) {
        if (!json.has(path)) throw IOException("Missing key in persistent storage: '$path'")
        try {
            field.set(owner, PersistentSerializer.fromJson(field, json.get(path)))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
