package org.oddlama.vane.util

import com.mojang.datafixers.DataFixUtils
import com.mojang.datafixers.types.Type
import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.datafix.DataFixers
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.crafting.RecipeHolder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Bridges Bukkit/Paper APIs to internal NMS handles.
 */
object Nms {
    /** Returns the NMS handle for a player. */
    @JvmStatic
    fun getPlayer(player: Player): ServerPlayer = (player as CraftPlayer).handle

    /** Returns the NMS handle for a Bukkit entity. */
    @JvmStatic
    fun entityHandle(entity: org.bukkit.entity.Entity): Entity = (entity as CraftEntity).handle

    /** Converts an NMS item stack to a Bukkit mirror stack. */
    fun bukkitItemStack(stack: net.minecraft.world.item.ItemStack?): ItemStack =
        CraftItemStack.asCraftMirror(stack)

    /** Returns or creates the NMS handle for a Bukkit item stack. */
    @JvmStatic
    fun itemHandle(itemStack: ItemStack?): net.minecraft.world.item.ItemStack? {
        itemStack ?: return null
        if (itemStack !is CraftItemStack) return CraftItemStack.asNMSCopy(itemStack)
        return try {
            val handle = CraftItemStack::class.java.getDeclaredField("handle")
                .also { it.isAccessible = true }
            handle.get(itemStack) as? net.minecraft.world.item.ItemStack
        } catch (_: NoSuchFieldException) {
            null
        } catch (_: IllegalAccessException) {
            null
        }
    }

    /** Returns the NMS handle for a player, or null when incompatible. */
    @JvmStatic
    fun playerHandle(player: Player?): ServerPlayer? = (player as? CraftPlayer)?.handle

    /** Returns the NMS handle for a world. */
    @JvmStatic
    fun worldHandle(world: World): ServerLevel = (world as CraftWorld).handle

    /** Returns the dedicated server handle. */
    @JvmStatic
    fun serverHandle(): DedicatedServer = (Bukkit.getServer() as CraftServer).server

    /**
     * Registers a custom entity type by cloning datafixer mappings from a base entity.
     */
    @JvmStatic
    fun registerEntity(
        baseEntityType: NamespacedKey,
        pseudoNamespace: String?,
        key: String?,
        builder: EntityType.Builder<*>
    ) {
        val id = "${pseudoNamespace}_$key"
        val worldVersion = SharedConstants.getCurrentVersion().dataVersion().version()
        val worldVersionKey = DataFixUtils.makeKey(worldVersion)

        @Suppress("UNCHECKED_CAST")
        val dataTypesMap = DataFixers.getDataFixer()
            .getSchema(worldVersionKey)
            .findChoiceType(References.ENTITY)
            .types() as MutableMap<Any, Type<*>?>
        dataTypesMap["minecraft:$id"] = dataTypesMap[baseEntityType.toString()]
        val rk: ResourceKey<EntityType<*>> =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace(id))
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(rk))
    }

    /** Spawns an NMS entity into a Bukkit world. */
    @JvmStatic
    fun spawn(world: World, entity: Entity) = worldHandle(world).addFreshEntity(entity)

    /** Unlocks all known recipes for a player. */
    @JvmStatic
    fun unlockAllRecipes(player: Player?): Int {
        val recipes: MutableCollection<RecipeHolder<*>> = serverHandle().recipeManager.getRecipes()
        return playerHandle(player)!!.awardRecipes(recipes)
    }

    /** Returns a sortable creative-tab index for an NMS item stack. */
    @JvmStatic
    fun creativeTabId(itemStack: net.minecraft.world.item.ItemStack): Int =
        CreativeModeTabs.allTabs().takeWhile { it.contains(itemStack) }.count()

    /** Sets a block to air without dropping loot. */
    @JvmStatic
    fun setAirNoDrops(block: Block) {
        worldHandle(block.world).getBlockEntity(BlockPos(block.x, block.y, block.z))
        block.setType(Material.AIR, false)
    }
}
