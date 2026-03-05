package org.oddlama.vane.portals.portal

import org.bukkit.block.Block
import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.util.LazyBlock
import java.io.IOException
import java.util.*

class PortalBlock(private val block: LazyBlock, private val type: Type?) {
    constructor(block: Block?, type: Type?) : this(LazyBlock(block), type)

    fun block(): Block? {
        return block.block()
    }

    fun type(): Type? {
        return type
    }

    fun lookup(portalId: UUID?): PortalBlockLookup {
        return PortalBlockLookup(portalId, type)
    }

    override fun hashCode(): Int {
        return block().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PortalBlock) {
            return false
        }

        // Only block is compared, as the same block can only have one functions.
        return block() == other.block()
    }

    enum class Type {
        ORIGIN,
        CONSOLE,
        BOUNDARY1,
        BOUNDARY2,
        BOUNDARY3,
        BOUNDARY4,
        BOUNDARY5,
        PORTAL,
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun serialize(o: Any): Any {
            val portalBlock = o as PortalBlock
            val json = JSONObject()
            json.put("block", PersistentSerializer.toJson(LazyBlock::class.java, portalBlock.block))
            json.put("type", PersistentSerializer.toJson(Type::class.java, portalBlock.type))
            return json
        }

        @JvmStatic
        @Throws(IOException::class)
        fun deserialize(o: Any): PortalBlock {
            val json = o as JSONObject
            // `PersistentSerializer.fromJson` signature: `fun <U> fromJson(cls: Class<U>?, value: Any?): U?`.
            // Do not include `?` in the generic parameter; the return type is already nullable.
            val block: LazyBlock? = PersistentSerializer.fromJson(LazyBlock::class.java, json.get("block"))
            val type: Type? = PersistentSerializer.fromJson(Type::class.java, json.get("type"))
            return PortalBlock(block!!, type)
        }
    }
}
