package org.oddlama.vane.trifles

import org.bukkit.plugin.Plugin
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.trifles.commands.Finditem
import org.oddlama.vane.trifles.commands.Heads
import org.oddlama.vane.trifles.commands.Setspawn
import org.oddlama.vane.trifles.items.*
import org.oddlama.vane.trifles.items.storage.Backpack
import org.oddlama.vane.trifles.items.storage.Pouch
import java.util.*

@VaneModule(name = "trifles", bstats = 8644, configVersion = 4, langVersion = 4, storageVersion = 1)
class Trifles : Module<Trifles?>() {
    val lastXpBottleConsumeTime: HashMap<UUID, Long> = HashMap()
    var xpBottles: XpBottles?
    var itemFinder: ItemFinder?
    var storageGroup: StorageGroup
    var packetEventsEnabled: Boolean = false

    init {
        val fastWalkingGroup = FastWalkingGroup(this)
        FastWalkingListener(fastWalkingGroup)
        DoubleDoorListener(this)
        ItemFrameListener(this)
        HarvestListener(this)
        RepairCostLimiter(this)
        RecipeUnlock(this)
        ChestSorter(this)
        itemFinder = ItemFinder(this)

        Heads(this)
        Setspawn(this)
        Finditem(this)

        PapyrusScroll(this)
        Scrolls(this)
        ReinforcedElytra(this)
        File(this)
        Sickles(this)
        EmptyXpBottle(this)
        xpBottles = XpBottles(this)
        Trowel(this)
        NorthCompass(this)
        SlimeBucket(this)

        storageGroup = StorageGroup(this)
        Pouch(storageGroup.getContext()!!)
        Backpack(storageGroup.getContext()!!)
    }

    override fun onModuleEnable() {
        module!!.scheduleNextTick {
            val packetEventsPlugin: Plugin? = module!!.server.pluginManager.getPlugin("PacketEvents")
            if (packetEventsPlugin != null && packetEventsPlugin.isEnabled) {
                packetEventsEnabled = true
                module!!.log.info("Enabling PacketEvents integration")
            }
        }
    }
}
