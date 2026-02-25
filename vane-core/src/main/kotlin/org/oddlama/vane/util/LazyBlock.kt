package org.oddlama.vane.util

import org.bukkit.Bukkit
import org.bukkit.block.Block
import java.util.*

class LazyBlock {
    private val worldId: UUID?
    private var x = 0
    private var y = 0
    private var z = 0
    private var block: Block?

    constructor(block: Block?) {
        if (block == null) {
            this.worldId = null
            this.x = 0
            this.y = 0
            this.z = 0
        } else {
            this.worldId = block.world.uid
            this.x = block.x
            this.y = block.y
            this.z = block.z
        }
        this.block = block
    }

    constructor(worldId: UUID?, x: Int, y: Int, z: Int) {
        this.worldId = worldId
        this.x = x
        this.y = y
        this.z = z
        this.block = null
    }

    fun worldId(): UUID? {
        return worldId
    }

    fun x(): Int {
        return x
    }

    fun y(): Int {
        return y
    }

    fun z(): Int {
        return z
    }

    fun block(): Block? {
        if (worldId != null && block == null) {
            this.block = Bukkit.getWorld(worldId)!!.getBlockAt(x, y, z)
        }

        return block
    }
}
