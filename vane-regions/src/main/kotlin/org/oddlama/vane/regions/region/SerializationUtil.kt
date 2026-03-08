package org.oddlama.vane.regions.region

import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer
import java.util.*

/**
 * Serializes the common ownable fields (id, name, owner) shared by [Region] and [RegionGroup].
 */
internal fun putOwnable(json: JSONObject, id: UUID?, name: String?, owner: UUID?) {
    json.put("id", PersistentSerializer.toJson(UUID::class.java, id))
    json.put("name", PersistentSerializer.toJson(String::class.java, name))
    json.put("owner", PersistentSerializer.toJson(UUID::class.java, owner))
}

/**
 * Deserializes the common ownable fields (id, name, owner) shared by [Region] and [RegionGroup].
 */
internal fun readOwnable(json: JSONObject): Triple<UUID?, String?, UUID?> {
    val id = PersistentSerializer.fromJson(UUID::class.java, json.get("id"))
    val name = PersistentSerializer.fromJson(String::class.java, json.get("name"))
    val owner = PersistentSerializer.fromJson(UUID::class.java, json.get("owner"))
    return Triple(id, name, owner)
}

/**
 * Wraps a [NoSuchFieldException]-throwing block, rethrowing as [RuntimeException] with a clear message.
 */
internal inline fun <T> withField(block: () -> T): T {
    return try {
        block()
    } catch (e: NoSuchFieldException) {
        throw RuntimeException("Invalid field. This is a bug.", e)
    }
}



