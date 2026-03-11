package org.oddlama.vane.core.persistent

import org.bukkit.persistence.PersistentDataContainer
import java.util.*

/**
 * Returns all UUIDs stored under keys that start with [prefix] in this [PersistentDataContainer].
 * Useful for loading/saving collections of serialized objects keyed by UUID.
 */
fun PersistentDataContainer.storedUuidsByPrefix(prefix: String): Set<UUID> =
    keys
        .filter { it.toString().startsWith(prefix) }
        .map { UUID.fromString(it.toString().substring(prefix.length)) }
        .toSet()
