package org.oddlama.vane.portals.portal

import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer
import java.io.IOException
import java.util.*

class PortalBlockLookup(private val portalId: UUID?, private val type: PortalBlock.Type?) {
    fun portalId(): UUID? {
        return portalId
    }

    fun type(): PortalBlock.Type? {
        return type
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun serialize(o: Any): Any {
            val lookup = o as PortalBlockLookup
            val json = JSONObject()
            json.put("portalId", PersistentSerializer.toJson(UUID::class.java, lookup.portalId))
            json.put("type", PersistentSerializer.toJson(PortalBlock.Type::class.java, lookup.type))
            return json
        }

        @JvmStatic
        @Throws(IOException::class)
        fun deserialize(o: Any): PortalBlockLookup {
            val json = o as JSONObject
            val portalId: UUID? = PersistentSerializer.fromJson(UUID::class.java, json.get("portalId"))
            val type: PortalBlock.Type? =
                PersistentSerializer.fromJson(PortalBlock.Type::class.java, json.get("type"))
            return PortalBlockLookup(portalId, type)
        }
    }
}
