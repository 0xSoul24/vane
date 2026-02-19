package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.Conversions.msToTicks;
import static org.oddlama.vane.util.ItemUtil.damageItem;
import static org.oddlama.vane.util.PlayerUtil.swingArm;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.trifles.event.PlayerTeleportScrollEvent;

public class Scrolls extends Listener<Trifles> {

    private Set<Scroll> scrolls = new HashSet<>();
    private Set<Material> baseMaterials = new HashSet<>();

    @ConfigInt(
        def = 15000,
        min = 0,
        desc = "A cooldown in milliseconds that is applied when the player takes damage (prevents combat logging). Set to 0 to allow combat logging."
    )
    private int configDamageCooldown;

    public Scrolls(Context<Trifles> context) {
        super(context.group("Scrolls", "Several scrolls that allow player teleportation, and related behavior."));
        scrolls.add(new HomeScroll(getContext()));
        scrolls.add(new UnstableScroll(getContext()));
        scrolls.add(new SpawnScroll(getContext()));
        scrolls.add(new LodestoneScroll(getContext()));
        scrolls.add(new DeathScroll(getContext()));

        // Accumulate base materials so the cooldown can be applied to all scrolls regardless of
        // base material.
        for (final var scroll : scrolls) {
            baseMaterials.add(scroll.baseMaterial());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false) // ignoreCancelled = false to catch right-click-air events
    public void onPlayerRightClick(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        // Assert this is a matching custom item
        final var player = event.getPlayer();
        final var item = player.getEquipment().getItem(event.getHand());
        final var customItem = getModule().core.itemRegistry().get(item);
        if (!(customItem instanceof Scroll scroll) || !scroll.enabled()) {
            return;
        }

        // Never actually use the base item if it's custom!
        event.setUseItemInHand(Event.Result.DENY);

        switch (event.getAction()) {
            default:
                return;
            case RIGHT_CLICK_AIR:
                break;
            case RIGHT_CLICK_BLOCK:
                // Require non-canceled state (so it won't trigger for block-actions like chests, doors, etc.)
                // The event system properly handles block interactions and sets useInteractedBlock accordingly
                if (event.useInteractedBlock() != Event.Result.DENY) {
                    return;
                }
                break;
        }

        final var toLocation = scroll.teleportLocation(item, player, true);
        if (toLocation == null) {
            return;
        }

        // Check cooldown
        if (player.getCooldown(scroll.baseMaterial()) > 0) {
            return;
        }

        final var currentLocation = player.getLocation();
        if (teleportFromScroll(player, currentLocation, toLocation)) {
            // Set cooldown
            cooldownAll(player, scroll.configCooldown);

            // Damage item
            damageItem(player, item, 1);
            swingArm(player, event.getHand());
        }
    }

    public boolean teleportFromScroll(final Player player, final Location from, final Location to) {
        // Send scroll teleport event
        final var teleportScrollEvent = new PlayerTeleportScrollEvent(player, from, to);
        getModule().getServer().getPluginManager().callEvent(teleportScrollEvent);
        if (teleportScrollEvent.isCancelled()) {
            return false;
        }

        // Teleport
        player.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN);

        // Play sounds
        from.getWorld().playSound(from, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.1f);
        to.getWorld().playSound(to, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.1f);
        from.getWorld().playSound(from, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f);
        to.getWorld().playSound(to, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // Create particles
        from.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0.0, 1.0, 0.0), 200, 1.0, 2.0, 1.0, 1.0);
        to.getWorld().spawnParticle(Particle.END_ROD, to.clone().add(0.0, 1.0, 0.0), 100, 1.0, 2.0, 1.0, 1.0);
        return true;
    }

    public void cooldownAll(final Player player, int cooldownMs) {
        final var cooldownTicks = (int) msToTicks(cooldownMs);
        for (final var mat : baseMaterials) {
            // Don't ever decrease cooldown
            if (player.getCooldown(mat) < cooldownTicks) {
                player.setCooldown(mat, cooldownTicks);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTakeDamage(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            cooldownAll(player, configDamageCooldown);
        }
    }
}
