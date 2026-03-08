package org.oddlama.vane.regions.menu

import java.util.*

class RegionMenuTag(private val regionId: UUID?) {
    fun regionId(): UUID? {
        return regionId
    }
}
