package org.oddlama.vane.core.persistent

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import java.util.*
import java.util.stream.Collectors

/**
 * Returns all UUIDs stored under keys that start with [prefix] in this [PersistentDataContainer].
 * Useful for loading/saving collections of serialized objects keyed by UUID.
 */
fun PersistentDataContainer.storedUuidsByPrefix(prefix: String): Set<UUID> =
    keys
        .stream()
        .filter { key: NamespacedKey? -> key.toString().startsWith(prefix) }
        .map { key: NamespacedKey? -> key.toString().substring(prefix.length) }
        .map { uuid: String? -> UUID.fromString(uuid) }
        .collect(Collectors.toSet())

