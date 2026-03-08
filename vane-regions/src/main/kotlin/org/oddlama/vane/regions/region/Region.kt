package org.oddlama.vane.regions.region

import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.regions.Regions
import java.io.IOException
import java.util.*

class Region {
    private constructor()

    constructor(name: String?, owner: UUID?, extent: RegionExtent?, regionGroup: UUID?) {
        this.id = UUID.randomUUID()
        this.name = name
        this.owner = owner
        this.extent = extent
        this.regionGroup = regionGroup
    }

    private var id: UUID? = null
    private var name: String? = null
    private var owner: UUID? = null
    private var extent: RegionExtent? = null
    private var regionGroup: UUID? = null

    @JvmField
    var invalidated: Boolean = true

    fun id(): UUID? {
        return id
    }

    fun name(): String? {
        return name
    }

    fun name(name: String?) {
        this.name = name
        this.invalidated = true
    }

    fun owner(): UUID? {
        return owner
    }

    fun extent(): RegionExtent? {
        return extent
    }

    private var cachedRegionGroup: RegionGroup? = null

    fun regionGroupId(): UUID? {
        return regionGroup
    }

    fun regionGroupId(regionGroup: UUID?) {
        this.regionGroup = regionGroup
        this.cachedRegionGroup = null
        this.invalidated = true
    }

    fun regionGroup(regions: Regions): RegionGroup? {
        if (cachedRegionGroup == null) {
            cachedRegionGroup = regions.getRegionGroup(regionGroup)
        }
        return cachedRegionGroup
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun serialize(o: Any): Any {
            val region = o as Region
            val json = JSONObject()
            putOwnable(json, region.id, region.name, region.owner)
            json.put("regionGroup", PersistentSerializer.toJson(UUID::class.java, region.regionGroup))
            json.put("extent", PersistentSerializer.toJson(RegionExtent::class.java, region.extent))
            return json
        }

        @JvmStatic
        @Throws(IOException::class)
        fun deserialize(o: Any): Region {
            val json = o as JSONObject
            val region = Region()
            val (id, name, owner) = readOwnable(json)
            region.id = id
            region.name = name
            region.owner = owner
            region.regionGroup = PersistentSerializer.fromJson(UUID::class.java, json.get("regionGroup"))
            region.extent = PersistentSerializer.fromJson(RegionExtent::class.java, json.get("extent"))
            return region
        }
    }
}
