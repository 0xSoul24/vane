package org.oddlama.vane.util

import org.bukkit.Bukkit
import org.bukkit.block.Block
import java.util.*

class LazyBlock(
    val worldId: UUID?,
    val x: Int,
    val y: Int,
    val z: Int,
) {
    private var block: Block? = null

    constructor(block: Block?) : this(
        worldId = block?.world?.uid,
        x = block?.x ?: 0,
        y = block?.y ?: 0,
        z = block?.z ?: 0,
    ) {
        this.block = block
    }

    fun block(): Block? {
        if (worldId != null && block == null) {
            block = Bukkit.getWorld(worldId)!!.getBlockAt(x, y, z)
        }
        return block
    }
}
