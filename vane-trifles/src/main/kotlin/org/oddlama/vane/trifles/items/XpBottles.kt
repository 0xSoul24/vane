package org.oddlama.vane.trifles.items

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.EquipmentSlot
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.item.CustomItem
import org.oddlama.vane.core.item.CustomItemHelper
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.expForLevel
import org.oddlama.vane.util.PlayerUtil
import java.util.*

class XpBottles(context: Context<Trifles?>) :
    Listener<Trifles?>(context.group("XpBottles", "Several XP bottles storing a certain amount of experience.")) {
    abstract class XpBottle(context: Context<Trifles?>) : CustomItem<Trifles?>(context) {
        @JvmField
        @ConfigInt(def = -1, desc = "Level capacity.")
        var configCapacity: Int = 0

        override fun displayName(): Component? {
            return super.displayName()!!.color(NamedTextColor.YELLOW)
        }

        override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
            return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.USE_OFFHAND)
        }
    }

    @VaneItem(name = "small_xp_bottle", base = Material.HONEY_BOTTLE, modelData = 0x76000b, version = 1)
    class SmallXpBottle(context: Context<Trifles?>) : XpBottle(context) {
        fun configCapacityDef(): Int {
            return 10
        }
    }

    @VaneItem(name = "medium_xp_bottle", base = Material.HONEY_BOTTLE, modelData = 0x76000c, version = 1)
    class MediumXpBottle(context: Context<Trifles?>) : XpBottle(context) {
        fun configCapacityDef(): Int {
            return 20
        }
    }

    @VaneItem(name = "large_xp_bottle", base = Material.HONEY_BOTTLE, modelData = 0x76000d, version = 1)
    class LargeXpBottle(context: Context<Trifles?>) : XpBottle(context) {
        fun configCapacityDef(): Int {
            return 30
        }
    }

    @JvmField
    var bottles: MutableList<XpBottle> = ArrayList()

    init {
        bottles.add(SmallXpBottle(getContext()!!))
        bottles.add(MediumXpBottle(getContext()!!))
        bottles.add(LargeXpBottle(getContext()!!))
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        val item = event.item
        val customItem: org.oddlama.vane.core.item.api.CustomItem? = module!!.core?.itemRegistry()?.get(item)
        if (customItem !is XpBottle || !customItem.enabled()) {
            return
        }

        // Exchange items
        val player = event.getPlayer()
        val mainHand = item == player.inventory.itemInMainHand
        PlayerUtil.removeOneItemFromHand(player, if (mainHand) EquipmentSlot.HAND else EquipmentSlot.OFF_HAND)
        PlayerUtil.giveItem(player, CustomItemHelper.newStack("vane_trifles:empty_xp_bottle"))

        // Add player experience without applying mending effects
        module!!.lastXpBottleConsumeTime[player.uniqueId] = System.currentTimeMillis()
        player.giveExp(expForLevel(customItem.configCapacity), false)
        player
            .world
            .playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f)

        // Do not consume actual base item
        event.isCancelled = true
    }
}
