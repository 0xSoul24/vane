package org.oddlama.vane.proxycore.config

import java.util.*
import java.util.stream.Collectors

class AuthMultiplex(@JvmField var port: Int?, allowedUuids: MutableList<String?>?) {
    private val allowedUuids: MutableList<UUID?>

    init {
        if (allowedUuids.isNullOrEmpty()) {
            this.allowedUuids = mutableListOf()
        } else {
            this.allowedUuids = allowedUuids
                .stream()
                .filter { s: String? -> !s!!.isEmpty() }
                .map { name: String? -> UUID.fromString(name) }
                .collect(Collectors.toList())
        }
    }

    fun uuidIsAllowed(uuid: UUID?): Boolean {
        return allowedUuids.isEmpty() || allowedUuids.contains(uuid)
    }
}
