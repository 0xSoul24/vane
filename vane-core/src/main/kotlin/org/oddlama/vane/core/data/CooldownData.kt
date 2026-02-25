package org.oddlama.vane.core.data

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

class CooldownData(private val key: NamespacedKey, private val cooldownTime: Long) {
    /**
     * Updates the cooldown data if and only if the cooldown has been exceeded.
     * 
     * @return returns true if cooldownTime has been exceeded.
     */
    fun checkOrUpdateCooldown(holder: PersistentDataHolder): Boolean {
        val persistentData = holder.persistentDataContainer
        val lastTime = persistentData.getOrDefault(key, PersistentDataType.LONG, 0L)
        val now = System.currentTimeMillis()
        if (now - lastTime < cooldownTime) {
            return false
        }

        persistentData.set(key, PersistentDataType.LONG, now)
        return true
    }

    /**
     * @return Gets the status of the cooldown without updating
     */
    fun peekCooldown(holder: PersistentDataHolder): Boolean {
        val persistentData = holder.persistentDataContainer
        val lastTime = persistentData.getOrDefault(this.key, PersistentDataType.LONG, 0L)
        val now = System.currentTimeMillis()
        return now - lastTime >= this.cooldownTime
    }

    fun clear(holder: PersistentDataHolder) {
        val persistentData = holder.persistentDataContainer
        persistentData.remove(key)
    }
}
