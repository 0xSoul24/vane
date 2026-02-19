package org.oddlama.vane.util;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;

public class LazyBlock {

    private final UUID worldId;
    private int x;
    private int y;
    private int z;
    private Block block;

    public LazyBlock(final Block block) {
        if (block == null) {
            this.worldId = null;
            this.x = 0;
            this.y = 0;
            this.z = 0;
        } else {
            this.worldId = block.getWorld().getUID();
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
        }
        this.block = block;
    }

    public LazyBlock(final UUID worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = null;
    }

    public UUID worldId() {
        return worldId;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public Block block() {
        if (worldId != null && block == null) {
            this.block = Bukkit.getWorld(worldId).getBlockAt(x, y, z);
        }

        return block;
    }
}
