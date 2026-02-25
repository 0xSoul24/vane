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
import net.minecraft.world.item.CreativeModeTab
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

object Nms {
    @JvmStatic
    fun getPlayer(player: Player): ServerPlayer? {
        return (player as CraftPlayer).handle
    }

    @JvmStatic
    fun entityHandle(entity: org.bukkit.entity.Entity): Entity? {
        return (entity as CraftEntity).handle
    }

    fun bukkitItemStack(stack: net.minecraft.world.item.ItemStack?): ItemStack {
        return CraftItemStack.asCraftMirror(stack)
    }


    @JvmStatic
    fun itemHandle(itemStack: ItemStack?): net.minecraft.world.item.ItemStack? {
        if (itemStack == null) {
            return null
        }

        if (itemStack !is CraftItemStack) {
            return CraftItemStack.asNMSCopy(itemStack)
        }

        try {
            val handle = CraftItemStack::class.java.getDeclaredField("handle")
            handle.setAccessible(true)
            return handle.get(itemStack) as net.minecraft.world.item.ItemStack?
        } catch (e: NoSuchFieldException) {
            return null
        } catch (e: IllegalAccessException) {
            return null
        }
    }

    @JvmStatic
    fun playerHandle(player: Player?): ServerPlayer? {
        if (player !is CraftPlayer) {
            return null
        }
        return player.handle
    }

    @JvmStatic
    fun worldHandle(world: World): ServerLevel? {
        return (world as CraftWorld).handle
    }

    @JvmStatic
    fun serverHandle(): DedicatedServer? {
        val bukkitServer = Bukkit.getServer()
        return (bukkitServer as CraftServer).server
    }

    @JvmStatic
    fun registerEntity(
        baseEntityType: NamespacedKey,
        pseudoNamespace: String?,
        key: String?,
        builder: EntityType.Builder<*>
    ) {
        val id = pseudoNamespace + "_" + key
        // From:
        // https://papermc.io/forums/t/register-and-spawn-a-custom-entity-on-1-13-x/293,
        // adapted for 1.18
        // Get the datafixer
        val worldVersion = SharedConstants.getCurrentVersion().dataVersion().version()
        val worldVersionKey = DataFixUtils.makeKey(worldVersion)
        val dataTypes: MutableMap<*, Type<*>?>? = DataFixers.getDataFixer()
            .getSchema(worldVersionKey)
            .findChoiceType(References.ENTITY)
            .types()
        @Suppress("UNCHECKED_CAST")
        val dataTypesMap = dataTypes as MutableMap<Any, Type<*>?>
        // Inject the new custom entity (this registers the key/id with the server,
        // so it will be available in vanilla constructs like the /summon command)
        dataTypesMap["minecraft:$id"] = dataTypesMap[baseEntityType.toString()]
        // Store a new type in registry
        val rk: ResourceKey<EntityType<*>> =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace(id))
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(rk))
    }

    @JvmStatic
    fun spawn(world: World, entity: Entity) {
        worldHandle(world)!!.addFreshEntity(entity)
    }

    @JvmStatic
    fun unlockAllRecipes(player: Player?): Int {
        val recipes: MutableCollection<RecipeHolder<*>> = serverHandle()!!.recipeManager.getRecipes()
        return playerHandle(player)!!.awardRecipes(recipes)
    }

    @JvmStatic
    fun creativeTabId(itemStack: net.minecraft.world.item.ItemStack): Int {
        // TODO FIXME BUG this is broken and always returns 0
        return CreativeModeTabs.allTabs().stream().takeWhile { tab: CreativeModeTab? -> tab!!.contains(itemStack) }
            .count().toInt()
    }

    @JvmStatic
    fun setAirNoDrops(block: Block) {
        val entity = worldHandle(block.world)!!.getBlockEntity(
            BlockPos(block.x, block.y, block.z)
        )
        block.setType(Material.AIR, false)
    }
}
