package org.oddlama.vane.core.data;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class CooldownData {

    private final NamespacedKey key;
    private final long cooldownTime;

    public CooldownData(NamespacedKey key, long cooldownTime) {
        this.key = key;
        this.cooldownTime = cooldownTime;
    }

    /**
     * Updates the cooldown data if and only if the cooldown has been exceeded.
     *
     * @return returns true if cooldownTime has been exceeded.
     */
    public boolean checkOrUpdateCooldown(final PersistentDataHolder holder) {
        final var persistentData = holder.getPersistentDataContainer();
        final var lastTime = persistentData.getOrDefault(key, PersistentDataType.LONG, 0L);
        final var now = System.currentTimeMillis();
        if (now - lastTime < cooldownTime) {
            return false;
        }

        persistentData.set(key, PersistentDataType.LONG, now);
        return true;
    }

    /**
     * @return Gets the status of the cooldown without updating
     */
    public boolean peekCooldown(PersistentDataHolder holder) {
        PersistentDataContainer persistentData = holder.getPersistentDataContainer();
        Long lastTime = persistentData.getOrDefault(this.key, PersistentDataType.LONG, 0L);
        long now = System.currentTimeMillis();
        return now - lastTime >= this.cooldownTime;
    }

    public void clear(final PersistentDataHolder holder) {
        final var persistentData = holder.getPersistentDataContainer();
        persistentData.remove(key);
    }
}
