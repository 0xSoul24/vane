package org.oddlama.vane.trifles;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.minecraft.world.entity.monster.Monster;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.core.Listener;

public class FastWalkingListener extends Listener<Trifles> {

    FastWalkingGroup fastWalking;

    public FastWalkingListener(FastWalkingGroup context) {
        super(context);
        this.fastWalking = context;
    }

    @ConfigBoolean(def = false, desc = "Whether hostile mobs should be allowed to fast walk on paths.")
    public boolean configHostileSpeedwalk;

    @ConfigBoolean(def = true, desc = "Whether villagers should be allowed to fast walk on paths.")
    public boolean configVillagerSpeedwalk;

    @ConfigBoolean(
        def = false,
        desc = "Whether players should be the only entities allowed to fast walk on paths (will override other path walk settings)."
    )
    public boolean configPlayersOnlySpeedwalk;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        // Players mustn't be flying
        final var player = event.getPlayer();
        if (player.isGliding()) {
            return;
        }

        LivingEntity effectEntity = player;
        if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity vehicle) {
            effectEntity = vehicle;
        }

        // Inspect a block type just a little below
        var block = effectEntity.getLocation().clone().subtract(0.0, 0.1, 0.0).getBlock();
        if (!fastWalking.configMaterials.contains(block.getType())) {
            return;
        }

        // Apply potion effect
        effectEntity.addPotionEffect(fastWalking.walkSpeedEffect);
    }

    // This is fired for entities except players.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityMove(final EntityMoveEvent event) {
        final var entity = event.getEntity();

        // Cancel event if speedwalking is only enabled for players
        if (configPlayersOnlySpeedwalk) return;

        // Cancel event if speedwalking is disabled for Hostile mobs
        if (entity instanceof Monster && !configHostileSpeedwalk) return;

        // Cancel event if speedwalking is disabled for villagers
        if (entity.getType() == EntityType.VILLAGER && !configVillagerSpeedwalk) return;

        // Inspect a block type just a little below
        var block = event.getTo().clone().subtract(0.0, 0.1, 0.0).getBlock();
        if (!fastWalking.configMaterials.contains(block.getType())) {
            return;
        }

        // Apply potion effect
        entity.addPotionEffect(fastWalking.walkSpeedEffect);
    }
}
