package org.oddlama.vane.portals.entity

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import org.bukkit.Location
import org.bukkit.World
import org.oddlama.vane.util.Nms

class FloatingItem(entitytypes: EntityType<out ItemEntity>, world: Level) : ItemEntity(
    entitytypes,
    world
) {
    constructor(location: Location) : this(location.getWorld(), location.x, location.y, location.z)

    constructor(world: World, x: Double, y: Double, z: Double) : this(EntityType.ITEM, Nms.worldHandle(world)) {
        setPos(x, y, z)
    }

    init {
        isSilent = true
        isInvulnerable = true
        isNoGravity = true
        // setSneaking(true); // Names would then only visible on direct line of sight BUT much
        // darker and offset by -0.5 in y direction
        setNeverPickUp()
        setUnlimitedLifetime()
        persist = false
        noPhysics = true
    }

    override fun isAlive(): Boolean {
        // Required to efficiently prevent hoppers and hopper minecarts from picking this up
        return false
    }

    override fun isAttackable(): Boolean {
        return false
    }

    override fun isCollidable(ignoreClimbing: Boolean): Boolean {
        return false
    }

    override fun isInvisible(): Boolean {
        return true
    }

    override fun fireImmune(): Boolean {
        return true
    }

    override fun tick() {}

    override fun inactiveTick() {}

    // Don't save or load
    public override fun readAdditionalSaveData(output: ValueInput) {}

    override fun addAdditionalSaveData(output: ValueOutput) {}

    override fun save(output: ValueOutput): Boolean {
        return false
    }

    override fun saveWithoutId(output: ValueOutput) {}

    override fun load(output: ValueInput) {}

    override fun setItem(itemStack: ItemStack) {
        super.setItem(itemStack)
        if (itemStack.hoverName.toFlatList().isEmpty()) {
            isCustomNameVisible = false
        } else {
            isCustomNameVisible = true
            customName = itemStack.hoverName
        }
    }
}