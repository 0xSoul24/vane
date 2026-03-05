package org.oddlama.vane.portals.event

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class EntityMoveEvent(@JvmField val entity: Entity?, @JvmField val from: Location?, @JvmField val to: Location?) : Event() {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic val handlerList: HandlerList = HandlerList()
    }
}
