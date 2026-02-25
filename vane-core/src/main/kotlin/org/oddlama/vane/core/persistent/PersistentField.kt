package org.oddlama.vane.core.persistent

import org.json.JSONObject
import java.io.IOException
import java.lang.reflect.Field
import java.util.function.Function

class PersistentField(private val owner: Any?, private val field: Field, mapName: Function<String?, String?>) {
    private val path: String? = mapName.apply(field.name.substring("storage".length))

    init {

        field.setAccessible(true)
    }

    fun path(): String? {
        return path
    }

    fun get(): Any? {
        try {
            return field.get(owner)
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }

    @Throws(IOException::class)
    fun save(json: JSONObject) {
        val value = PersistentSerializer.toJson(field, get()) ?: JSONObject.NULL
        json.put(path, value)
    }

    @Throws(IOException::class)
    fun load(json: JSONObject) {
        if (!json.has(path)) {
            throw IOException("Missing key in persistent storage: '$path'")
        }

        try {
            field.set(owner, PersistentSerializer.fromJson(field, json.get(path)))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }
}
