package org.oddlama.vane.portals

import com.google.common.collect.Sets
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.plugin.PluginManager
import org.bukkit.scheduler.BukkitTask
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.external.apache.commons.lang3.tuple.Pair
import org.oddlama.vane.portals.event.EntityMoveEvent
import java.util.*

class EntityMoveProcessor(context: Context<Portals?>?) : ModuleComponent<Portals?>(context) {
    // This is the queue of entity move events that need processing.
    // It is a linked hash map, so we can update moved entity positions
    // without changing iteration order. Processed entries will be removed from
    // the front, and new entities are added to the back. If an entity moves twice
    // but wasn't processed, we don't need to update it. This ensures that no entities
    // will be accidentally skipped when we are struggling to keep up.
    // This stores entity_id -> (entity, old location).
    private val moveEventProcessingQueue = LinkedHashMap<UUID?, Pair<Entity, Location?>>()

    // Two hash maps to store old and current positions for each entity.
    private var moveEventCurrentPositions = HashMap<UUID?, Pair<Entity?, Location?>?>()
    private var moveEventOldPositions = HashMap<UUID?, Pair<Entity?, Location?>?>()

    private var task: BukkitTask? = null

    private fun processEntityMovements() {
        // This custom event detector is necessary as PaperMC's entity move events trigger for
        // LivingEntities,
        // but we need move events for all entities. Wanna throw that potion through the portal?
        // Yes. Shoot players through a portal? Ohh, definitely. Throw junk right into their bases?
        // Abso-fucking-lutely.

        // This implementation uses a priority queue and a small
        // scheduling algorithm to prevent this function from ever causing lags.
        // Lags caused by other plugins or external means will inherently cause
        // the entity movement event tickrate to be slowed down.
        //
        // This function is called every tick and has two main phases.
        //
        // 1. Detect entity movement and queue entities for processing.
        // 2. Iterate through entities that moved in FIFO order
        //    and call event handlers, but make sure to immediately abort
        //    processing after exceeding a threshold time. This ensures
        //    that it will always at least process one entity, but never
        //    hog any performance from other tasks.

        // Phase 1 - Movement detection
        // --------------------------------------------

        val activePortalWorlds = HashSet<UUID>()
        for (portal in module!!.allAvailablePortals().filterNotNull()) {
            if (module!!.isActivated(portal)) {
                val worldId = portal.spawnWorld()
                if (worldId != null) {
                    activePortalWorlds.add(worldId)
                }
            }
        }

        // Store current positions for each entity
        for (worldId in activePortalWorlds) {
            val world: World? = module!!.server.getWorld(worldId)
            if (world != null) {
                for (entity in world.entities) {
                    moveEventCurrentPositions[entity.uniqueId] = Pair.of<Entity?, Location?>(entity, entity.location)
                }
            }
        }

        // For each entity that has an old position (computed efficiently via Sets.intersection),
        // but isn't yet contained in the entities to process, we check whether the position
        // has changed. If so, we add the entity to the processing queue.
        // If the processing queue already contained the entity, we remove it before iterating
        // as there is nothing to do - we simply lose information about the intermediate position.
        for (eid in Sets.difference(
            Sets.intersection(moveEventOldPositions.keys, moveEventCurrentPositions.keys),
            moveEventProcessingQueue.keys
        )) {
            val oldEntityAndLoc = moveEventOldPositions[eid]
            val newEntityAndLoc = moveEventCurrentPositions[eid]
            if (oldEntityAndLoc == null || newEntityAndLoc == null || !isMovement(
                    oldEntityAndLoc.getRight()!!,
                    newEntityAndLoc.getRight()!!
                )
            ) {
                continue
            }

            // oldEntityAndLoc contains nullable entity/location; the processing queue requires a non-null entity
            moveEventProcessingQueue[eid] = Pair.of(oldEntityAndLoc.getLeft()!!, oldEntityAndLoc.getRight())
        }

        // Swap old and current position hash maps, and only retain the now-old positions.
        // This avoids unnecessary allocations.
        val tmp = moveEventCurrentPositions
        moveEventCurrentPositions = moveEventOldPositions
        moveEventOldPositions = tmp
        moveEventCurrentPositions.clear()

        // Phase 2 - Event dispatching
        // --------------------------------------------
        val timeBegin = System.nanoTime()
        val pm: PluginManager = module!!.server.pluginManager
        val iter: MutableIterator<MutableMap.MutableEntry<UUID?, Pair<Entity, Location?>>?> =
            moveEventProcessingQueue.entries.iterator()
        while (iter.hasNext()) {
            val eAndOldLoc = iter.next()!!.value
            iter.remove()

            // Dispatch event.
            val entity = eAndOldLoc.getLeft()
            val event = EntityMoveEvent(entity, eAndOldLoc.getRight(), entity.location)
            pm.callEvent(event)

            // Abort if we exceed the threshold time
            val timeNow = System.nanoTime()
            if (timeNow - timeBegin > MOVE_EVENT_MAX_NANOSECONDS_PER_TICK) {
                break
            }
        }
    }

    override fun onEnable() {
        // Each tick we need to recalculate whether entities moved.
        // This is using a scheduling algorithm (see function implementation) to
        // keep it lightweight and to prevent lags.
        task = scheduleTaskTimer({ this.processEntityMovements() }, 1L, 1L)
    }

    override fun onDisable() {
        task!!.cancel()
    }

    companion object {
        // Never process entity-move events for more than ~30% of a tick.
        // We use 15ms threshold time, and 50ms would be 1 tick.
        private const val MOVE_EVENT_MAX_NANOSECONDS_PER_TICK = 15000000L

        private fun isMovement(l1: Location, l2: Location): Boolean {
            // Different worlds = not a movement event.
            return (l1.getWorld() === l2.getWorld() &&
                    (l1.x != l2.x || l1.y != l2.y || l1.z != l2.z || l1.pitch != l2.pitch || l1.yaw != l2.yaw)
                    )
        }
    }
}
