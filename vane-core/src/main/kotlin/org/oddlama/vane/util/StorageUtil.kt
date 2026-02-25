package org.oddlama.vane.util

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.*

object StorageUtil {
    @JvmStatic
    fun namespacedKey(namespace: String, key: String): NamespacedKey {
        return NamespacedKey(namespace, key)
    }

    fun subkey(key: NamespacedKey, sub: String?): NamespacedKey {
        return namespacedKey(key.namespace(), key.value() + "." + sub)
    }

    fun storageHasLocation(data: PersistentDataContainer, key: NamespacedKey): Boolean {
        return data.has(subkey(key, "world"), PersistentDataType.STRING)
    }

    @JvmStatic
    fun storageGetLocation(
        data: PersistentDataContainer,
        key: NamespacedKey,
        def: Location?
    ): Location? {
        try {
            val worldId = data.get(subkey(key, "world"), PersistentDataType.STRING)
            val x = data.get(subkey(key, "x"), PersistentDataType.DOUBLE)
            val y = data.get(subkey(key, "y"), PersistentDataType.DOUBLE)
            val z = data.get(subkey(key, "z"), PersistentDataType.DOUBLE)
            val yaw = data.get(subkey(key, "yaw"), PersistentDataType.FLOAT)
            val pitch = data.get(subkey(key, "pitch"), PersistentDataType.FLOAT)
            val world = Bukkit.getWorld(UUID.fromString(worldId)) ?: return def
            return Location(world, x!!, y!!, z!!, yaw!!, pitch!!)
        } catch (e: IllegalArgumentException) {
            return def
        } catch (e: NullPointerException) {
            return def
        }
    }

    @JvmStatic
    fun storageRemoveLocation(data: PersistentDataContainer, key: NamespacedKey) {
        data.remove(subkey(key, "world"))
        data.remove(subkey(key, "x"))
        data.remove(subkey(key, "y"))
        data.remove(subkey(key, "z"))
        data.remove(subkey(key, "yaw"))
        data.remove(subkey(key, "pitch"))
    }

    @JvmStatic
    fun storageSetLocation(
        data: PersistentDataContainer,
        key: NamespacedKey,
        location: Location
    ) {
        data.set(
            subkey(key, "world"),
            PersistentDataType.STRING,
            location.getWorld().uid.toString()
        )
        data.set(subkey(key, "x"), PersistentDataType.DOUBLE, location.x)
        data.set(subkey(key, "y"), PersistentDataType.DOUBLE, location.y)
        data.set(subkey(key, "z"), PersistentDataType.DOUBLE, location.z)
        data.set(subkey(key, "yaw"), PersistentDataType.FLOAT, location.yaw)
        data.set(subkey(key, "pitch"), PersistentDataType.FLOAT, location.pitch)
    }
}
