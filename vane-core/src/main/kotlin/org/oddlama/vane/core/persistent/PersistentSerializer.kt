package org.oddlama.vane.core.persistent

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.json.JSONArray
import org.json.JSONObject
import org.oddlama.vane.util.LazyBlock
import org.oddlama.vane.util.LazyLocation
import org.oddlama.vane.util.MaterialUtil.materialFrom
import org.oddlama.vane.util.StorageUtil.namespacedKey
import java.io.IOException
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.util.*

object PersistentSerializer {
    @Throws(IOException::class)
    private fun serializeNamespacedKey(o: Any): Any {
        return (o as NamespacedKey).toString()
    }

    @Throws(IOException::class)
    private fun deserializeNamespacedKey(o: Any): NamespacedKey {
        val s = (o as String).split(":").dropLastWhile { it.isEmpty() }.toTypedArray()
        if (s.size != 2) {
            throw IOException("Invalid namespaced key '$s'")
        }
        return namespacedKey(s[0], s[1])
    }

    @Throws(IOException::class)
    private fun serializeLazyLocation(o: Any): Any {
        val lazyLocation = o as LazyLocation
        val location = lazyLocation.location()
        val json = JSONObject()
        json.put("worldId", toJson(UUID::class.java, lazyLocation.worldId()))
        json.put("x", toJson(Double::class.javaPrimitiveType as Class<*>?, location.x as Any?))
        json.put("y", toJson(Double::class.javaPrimitiveType as Class<*>?, location.y as Any?))
        json.put("z", toJson(Double::class.javaPrimitiveType as Class<*>?, location.z as Any?))
        json.put("pitch", toJson(Float::class.javaPrimitiveType as Class<*>?, location.pitch as Any?))
        json.put("yaw", toJson(Float::class.javaPrimitiveType as Class<*>?, location.yaw as Any?))
        return json
    }

    @Throws(IOException::class)
    private fun deserializeLazyLocation(o: Any): LazyLocation {
        val json = o as JSONObject
        val worldId = fromJson(UUID::class.java, json.get("worldId"))
        val x = fromJson(Double::class.javaPrimitiveType as Class<*>?, json.get("x"))
        val y = fromJson(Double::class.javaPrimitiveType as Class<*>?, json.get("y"))
        val z = fromJson(Double::class.javaPrimitiveType as Class<*>?, json.get("z"))
        val pitch = fromJson(Float::class.javaPrimitiveType as Class<*>?, json.get("pitch"))
        val yaw = fromJson(Float::class.javaPrimitiveType as Class<*>?, json.get("yaw"))
        return LazyLocation(worldId, x!! as Double, y!! as Double, z!! as Double, yaw!! as Float, pitch!! as Float)
    }

    @Throws(IOException::class)
    private fun serializeLazyBlock(o: Any): Any {
        val lazyBlock = o as LazyBlock
        val json = JSONObject()
        json.put("worldId", toJson(UUID::class.java, lazyBlock.worldId()))
        json.put("x", toJson(Int::class.javaPrimitiveType as Class<*>?, lazyBlock.x() as Any?))
        json.put("y", toJson(Int::class.javaPrimitiveType as Class<*>?, lazyBlock.y() as Any?))
        json.put("z", toJson(Int::class.javaPrimitiveType as Class<*>?, lazyBlock.z() as Any?))
        return json
    }

    @Throws(IOException::class)
    private fun deserializeLazyBlock(o: Any): LazyBlock {
        val json = o as JSONObject
        val worldId = fromJson(UUID::class.java, json.get("worldId"))
        val x = fromJson(Int::class.javaPrimitiveType as Class<*>?, json.get("x"))
        val y = fromJson(Int::class.javaPrimitiveType as Class<*>?, json.get("y"))
        val z = fromJson(Int::class.javaPrimitiveType as Class<*>?, json.get("z"))
        return LazyBlock(worldId, x!! as Int, y!! as Int, z!! as Int)
    }

    @Throws(IOException::class)
    private fun serializeMaterial(o: Any): Any? {
        return toJson(NamespacedKey::class.java as Class<*>?, (o as Material).key as Any?)
    }

    @Throws(IOException::class)
    private fun deserializeMaterial(o: Any): Material? {
        return materialFrom(fromJson(NamespacedKey::class.java, o) as NamespacedKey)
    }

    @Throws(IOException::class)
    private fun serializeItemStack(o: Any): Any {
        return Base64.getEncoder().encode((o as ItemStack).serializeAsBytes()).toString(StandardCharsets.UTF_8)
    }

    @Throws(IOException::class)
    private fun deserializeItemStack(o: Any): ItemStack {
        return ItemStack.deserializeBytes(Base64.getDecoder().decode((o as String).toByteArray(StandardCharsets.UTF_8)))
    }

    private fun isNull(o: Any?): Boolean {
        return o == null || o === JSONObject.NULL
    }

    @JvmField
    val serializers: MutableMap<Class<*>?, Function<Any?, Any?>> = HashMap()
    @JvmField
    val deserializers: MutableMap<Class<*>?, Function<Any?, Any?>> = HashMap()

    init {
        // Primitive types
        serializers[Boolean::class.javaPrimitiveType] = Function { obj -> obj.toString() }
        serializers[Char::class.javaPrimitiveType] = Function { obj -> obj.toString() }
        serializers[Double::class.javaPrimitiveType] = Function { obj -> obj.toString() }
        serializers[Float::class.javaPrimitiveType] = Function { obj -> obj.toString() }
        serializers[Int::class.javaPrimitiveType] = Function { obj -> obj.toString() }
        serializers[Long::class.javaPrimitiveType] = Function { obj -> obj.toString() }
        serializers[Boolean::class.java] = Function { obj -> obj.toString() }
        serializers[Char::class.java] = Function { obj -> obj.toString() }
        serializers[Double::class.java] = Function { obj -> obj.toString() }
        serializers[Float::class.java] = Function { obj -> obj.toString() }
        serializers[Int::class.java] = Function { obj -> obj.toString() }
        serializers[Long::class.java] = Function { obj -> obj.toString() }

        deserializers[Boolean::class.javaPrimitiveType] = Function { x -> (x as String?).toBoolean() }
        deserializers[Char::class.javaPrimitiveType] = Function { x -> (x as String)[0] }
        deserializers[Double::class.javaPrimitiveType] = Function { x -> (x as String).toDouble() }
        deserializers[Float::class.javaPrimitiveType] = Function { x -> (x as String).toFloat() }
        deserializers[Int::class.javaPrimitiveType] = Function { x -> (x as String).toInt() }
        deserializers[Long::class.javaPrimitiveType] = Function { x -> (x as String).toLong() }
        deserializers[Boolean::class.java] = Function { x -> (x as String?).toBoolean() }
        deserializers[Char::class.java] = Function { x -> (x as String)[0] }
        deserializers[Double::class.java] = Function { x -> (x as String).toDouble() }
        deserializers[Float::class.java] = Function { x -> (x as String).toFloat() }
        deserializers[Int::class.java] = Function { x -> (x as String).toInt() }
        deserializers[Long::class.java] = Function { x -> (x as String).toLong() }

        // Other types
        serializers[String::class.java] = Function { x -> x }
        deserializers[String::class.java] = Function { x -> x }
        serializers[UUID::class.java] = Function { obj -> obj.toString() }
        deserializers[UUID::class.java] = Function { x -> UUID.fromString(x as String?) }

        // Bukkit types
        serializers[NamespacedKey::class.java] = Function { obj -> serializeNamespacedKey(obj!!) }
        deserializers[NamespacedKey::class.java] = Function { obj -> deserializeNamespacedKey(obj!!) }
        serializers[LazyLocation::class.java] = Function { obj -> serializeLazyLocation(obj!!) }
        deserializers[LazyLocation::class.java] = Function { obj -> deserializeLazyLocation(obj!!) }
        serializers[LazyBlock::class.java] = Function { obj -> serializeLazyBlock(obj!!) }
        deserializers[LazyBlock::class.java] = Function { obj -> deserializeLazyBlock(obj!!) }
        serializers[Material::class.java] = Function { obj -> serializeMaterial(obj!!) }
        deserializers[Material::class.java] = Function { obj -> deserializeMaterial(obj!!) }
        serializers[ItemStack::class.java] = Function { obj -> serializeItemStack(obj!!) }
        deserializers[ItemStack::class.java] = Function { obj -> deserializeItemStack(obj!!) }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun toJson(field: Field, value: Any?): Any? {
        return toJson(field.genericType, value)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun toJson(cls: Class<*>?, value: Any?): Any? {
        val serializer: Function<Any?, Any?> = serializers[cls]!!
        if (isNull(value)) {
            return JSONObject.NULL
        }
        return serializer.apply(value)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun toJson(type: Type?, value: Any?): Any? {
        if (isNull(value)) {
            return JSONObject.NULL
        }
        if (type is ParameterizedType) {
            val baseType = type.rawType
            val typeArgs = type.actualTypeArguments
            when (baseType) {
                MutableMap::class.java -> {
                    val kTypeArgs = typeArgs[0] as Class<*>?
                    val vTypeArgs = typeArgs[1]
                    val json = JSONObject()
                    for (e in (value as MutableMap<*, *>).entries) {
                        json.put(toJson(kTypeArgs, e.key) as String?, toJson(vTypeArgs, e.value!!))
                    }
                    return json
                }
                MutableSet::class.java -> {
                    val tTypeArgs = typeArgs[0]
                    val json = JSONArray()
                    for (t in (value as MutableSet<*>)) {
                        json.put(toJson(tTypeArgs, t!!))
                    }
                    return json
                }
                MutableList::class.java -> {
                    val tTypeArgs = typeArgs[0]
                    val json = JSONArray()
                    for (t in (value as MutableList<*>)) {
                        json.put(toJson(tTypeArgs, t!!))
                    }
                    return json
                }
                else -> throw IOException("Cannot serialize $type. This is a bug.")
            }
        } else {
            return toJson(type as Class<*>?, value)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun fromJson(field: Field, value: Any?): Any? {
        return fromJson(field.genericType, value)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun <U> fromJson(cls: Class<U>?, value: Any?): U? {
        val deserializer: Function<Any?, Any?> = deserializers[cls]!!
        if (isNull(value)) {
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return deserializer.apply(value) as U?
    }

    @JvmStatic
    @Throws(IOException::class)
    fun fromJson(type: Type?, json: Any?): Any? {
        if (isNull(json)) {
            return null
        }
        if (type is ParameterizedType) {
            val baseType = type.rawType
            val typeArgs = type.actualTypeArguments
            when (baseType) {
                MutableMap::class.java -> {
                    val kTypeArgs = typeArgs[0] as Class<*>?
                    val vTypeArgs = typeArgs[1]
                    val value = HashMap<Any?, Any?>()
                    for (key in (json as JSONObject).keySet()) {
                        value[fromJson(kTypeArgs, key)] = fromJson(vTypeArgs, json.get(key))
                    }
                    return value
                }
                MutableSet::class.java -> {
                    val tTypeArgs = typeArgs[0]
                    val value = HashSet<Any?>()
                    for (t in (json as JSONArray)) {
                        value.add(fromJson(tTypeArgs, t))
                    }
                    return value
                }
                MutableList::class.java -> {
                    val tTypeArgs = typeArgs[0]
                    val value = ArrayList<Any?>()
                    for (t in (json as JSONArray)) {
                        value.add(fromJson(tTypeArgs, t))
                    }
                    return value
                }
                else -> throw IOException("Cannot deserialize $type. This is a bug.")
            }
        } else {
            return fromJson(type as Class<*>?, json)
        }
    }

    fun interface Function<T1, R> {
        @Throws(IOException::class)
        fun apply(t1: T1?): R?
    }
}
