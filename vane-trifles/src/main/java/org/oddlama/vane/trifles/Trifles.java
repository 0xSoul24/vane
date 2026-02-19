package org.oddlama.vane.trifles;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.trifles.items.XpBottles;

import java.util.HashMap;
import java.util.UUID;

@VaneModule(name = "trifles", bstats = 8644, configVersion = 4, langVersion = 4, storageVersion = 1)
public class Trifles extends Module<Trifles> {

    public final HashMap<UUID, Long> lastXpBottleConsumeTime = new HashMap<>();
    public XpBottles xpBottles;
    public ItemFinder itemFinder;
    public StorageGroup storageGroup;
    public boolean packetEventsEnabled;

    public Trifles() {
        final var fastWalkingGroup = new FastWalkingGroup(this);
        new FastWalkingListener(fastWalkingGroup);
        new DoubleDoorListener(this);
		new ItemFrameListener(this);
        new HarvestListener(this);
        new RepairCostLimiter(this);
        new RecipeUnlock(this);
        new ChestSorter(this);
        itemFinder = new ItemFinder(this);

        new org.oddlama.vane.trifles.commands.Heads(this);
        new org.oddlama.vane.trifles.commands.Setspawn(this);
        new org.oddlama.vane.trifles.commands.Finditem(this);

        new org.oddlama.vane.trifles.items.PapyrusScroll(this);
        new org.oddlama.vane.trifles.items.Scrolls(this);
        new org.oddlama.vane.trifles.items.ReinforcedElytra(this);
        new org.oddlama.vane.trifles.items.File(this);
        new org.oddlama.vane.trifles.items.Sickles(this);
        new org.oddlama.vane.trifles.items.EmptyXpBottle(this);
        xpBottles = new XpBottles(this);
        new org.oddlama.vane.trifles.items.Trowel(this);
        new org.oddlama.vane.trifles.items.NorthCompass(this);
        new org.oddlama.vane.trifles.items.SlimeBucket(this);

        storageGroup = new StorageGroup(this);
        new org.oddlama.vane.trifles.items.storage.Pouch(storageGroup.getContext());
        new org.oddlama.vane.trifles.items.storage.Backpack(storageGroup.getContext());
    }

    @Override
    public void onModuleEnable() {
        getModule().scheduleNextTick(() -> {
            var packetEventsPlugin = getModule().getServer().getPluginManager().getPlugin("PacketEvents");

            if (packetEventsPlugin != null && packetEventsPlugin.isEnabled()) {
                packetEventsEnabled = true;
                getModule().log.info("Enabling PacketEvents integration");
            }
        });
    }
}
