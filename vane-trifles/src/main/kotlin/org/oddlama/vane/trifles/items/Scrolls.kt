package org.oddlama.vane.trifles.items

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.trifles.event.PlayerTeleportScrollEvent
import org.oddlama.vane.util.Conversions
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.PlayerUtil

class Scrolls(context: Context<Trifles?>) : Listener<Trifles?>(
    context.group(
        "Scrolls",
        "Several scrolls that allow player teleportation, and related behavior."
    )
) {
    private val scrolls: MutableSet<Scroll> = HashSet()
    private val baseMaterials: MutableSet<Material> = HashSet()

    @ConfigInt(
        def = 15000,
        min = 0,
        desc = "A cooldown in milliseconds that is applied when the player takes damage (prevents combat logging). Set to 0 to allow combat logging."
    )
    private val configDamageCooldown = 0

    init {
        scrolls.add(HomeScroll(getContext()!!))
        scrolls.add(UnstableScroll(getContext()!!))
        scrolls.add(SpawnScroll(getContext()!!))
        scrolls.add(LodestoneScroll(getContext()!!))
        scrolls.add(DeathScroll(getContext()!!))

        // Accumulate base materials so the cooldown can be applied to all scrolls regardless of
        // base material.
        for (scroll in scrolls) {
            baseMaterials.add(scroll.baseMaterial()!!)
        }
    }

    @EventHandler(
        priority = EventPriority.LOW,
        ignoreCancelled = false
    ) // ignoreCancelled = false to catch right-click-air events
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) {
            return
        }

        if (event.useItemInHand() == Event.Result.DENY) {
            return
        }

        // Assert this is a matching custom item
        val player = event.getPlayer()
        val item = player.equipment.getItem(event.hand!!)
        val customItem: CustomItem? = module!!.core?.itemRegistry()?.get(item)
        if (customItem !is Scroll || !customItem.enabled()) {
            return
        }

        // Never actually use the base item if it's custom!
        event.setUseItemInHand(Event.Result.DENY)

        when (event.action) {
            Action.RIGHT_CLICK_AIR -> {}
            Action.RIGHT_CLICK_BLOCK ->                 // Require non-canceled state (so it won't trigger for block-actions like chests, doors, etc.)
                // The event system properly handles block interactions and sets useInteractedBlock accordingly
                if (event.useInteractedBlock() != Event.Result.DENY) {
                    return
                }

            else -> return
        }

        val toLocation = customItem.teleportLocation(item, player, true) ?: return

        // Check cooldown
        if (player.getCooldown(customItem.baseMaterial()!!) > 0) {
            return
        }

        val currentLocation = player.location
        if (teleportFromScroll(player, currentLocation, toLocation)) {
            // Set cooldown
            cooldownAll(player, customItem.configCooldown)

            // Damage item
            ItemUtil.damageItem(player, item, 1)
            PlayerUtil.swingArm(player, event.hand!!)
        }
    }

    fun teleportFromScroll(player: Player, from: Location, to: Location): Boolean {
        // Send scroll teleport event
        val teleportScrollEvent = PlayerTeleportScrollEvent(player, from, to)
        module!!.server.pluginManager.callEvent(teleportScrollEvent)
        if (teleportScrollEvent.isCancelled) {
            return false
        }

        // Teleport
        player.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN)

        // Play sounds
        from.getWorld().playSound(from, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.1f)
        to.getWorld().playSound(to, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.1f)
        from.getWorld().playSound(from, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f)
        to.getWorld().playSound(to, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f)

        // Create particles
        from.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0.0, 1.0, 0.0), 200, 1.0, 2.0, 1.0, 1.0)
        to.getWorld().spawnParticle(Particle.END_ROD, to.clone().add(0.0, 1.0, 0.0), 100, 1.0, 2.0, 1.0, 1.0)
        return true
    }

    fun cooldownAll(player: Player, cooldownMs: Int) {
        val cooldownTicks = Conversions.msToTicks(cooldownMs.toLong()).toInt()
        for (mat in baseMaterials) {
            // Don't ever decrease cooldown
            if (player.getCooldown(mat) < cooldownTicks) {
                player.setCooldown(mat, cooldownTicks)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerTakeDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity is Player) {
            cooldownAll(entity, configDamageCooldown)
        }
    }
}
