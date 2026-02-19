package org.oddlama.vane.util;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Nms {

	public static ServerPlayer getPlayer(Player player) {
		return ((CraftPlayer)player).getHandle();
	}

	public static Entity entityHandle(final org.bukkit.entity.Entity entity) {
		return ((CraftEntity)entity).getHandle();
	}

	@NotNull
	public static org.bukkit.inventory.ItemStack bukkitItemStack(ItemStack stack) {
		return CraftItemStack.asCraftMirror(stack);
	}


	public static ItemStack itemHandle(org.bukkit.inventory.ItemStack itemStack) {
		if (itemStack == null) {
			return null;
		}

		if (!(itemStack instanceof CraftItemStack)) {
			return CraftItemStack.asNMSCopy(itemStack);
		}

		try {
			final var handle = CraftItemStack.class.getDeclaredField("handle");
			handle.setAccessible(true);
			return (ItemStack)handle.get(itemStack);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			return null;
		}
	}

	public static ServerPlayer playerHandle(org.bukkit.entity.Player player) {
		if (!(player instanceof CraftPlayer)) {
			return null;
		}
		return ((CraftPlayer)player).getHandle();
	}

	public static ServerLevel worldHandle(org.bukkit.World world) {
		return ((CraftWorld)world).getHandle();
	}

	public static DedicatedServer serverHandle() {
		final var bukkitServer = Bukkit.getServer();
		return ((CraftServer)bukkitServer).getServer();
	}

	@SuppressWarnings({"unchecked", "deprecation"})
	public static void registerEntity(
	    final NamespacedKey baseEntityType,
	    final String pseudoNamespace,
	    final String key,
        final EntityType.Builder<?> builder
    ) {
		final var id = pseudoNamespace + "_" + key;
		// From:
		// https://papermc.io/forums/t/register-and-spawn-a-custom-entity-on-1-13-x/293,
		// adapted for 1.18
		// Get the datafixer
		final var worldVersion = SharedConstants.getCurrentVersion().dataVersion().version();
		final var worldVersionKey = DataFixUtils.makeKey(worldVersion);
		final var dataTypes = DataFixers.getDataFixer()
		                           .getSchema(worldVersionKey)
		                           .findChoiceType(References.ENTITY)
		                           .types();
		final var dataTypesMap = (Map<Object, Type<?>>)dataTypes;
		// Inject the new custom entity (this registers the key/id with the server,
		// so it will be available in vanilla constructs like the /summon command)
		dataTypesMap.put("minecraft:" + id, dataTypesMap.get(baseEntityType.toString()));
		// Store a new type in registry
		final var rk = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace(id));
		Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(rk));
	}

	public static void spawn(org.bukkit.World world, Entity entity) {
		worldHandle(world).addFreshEntity(entity);
	}

	public static int unlockAllRecipes(final org.bukkit.entity.Player player) {
		final var recipes = serverHandle().getRecipeManager().getRecipes();
		return playerHandle(player).awardRecipes(recipes);
	}

	public static int creativeTabId(final ItemStack itemStack) {
		// TODO FIXME BUG this is broken and always returns 0
		return (int)CreativeModeTabs.allTabs().stream().takeWhile(tab -> tab.contains(itemStack)).count();
	}

	public static void setAirNoDrops(final org.bukkit.block.Block block) {
        final var entity = worldHandle(block.getWorld()).getBlockEntity(
            new BlockPos(block.getX(), block.getY(), block.getZ())
        );
		block.setType(Material.AIR, false);
	}
}
