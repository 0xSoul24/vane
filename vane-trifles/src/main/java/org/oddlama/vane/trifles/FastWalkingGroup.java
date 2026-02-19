package org.oddlama.vane.trifles;

import static org.oddlama.vane.util.Conversions.msToTicks;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleGroup;

public class FastWalkingGroup extends ModuleGroup<Trifles> {

    @ConfigInt(def = 2, min = 1, max = 10, desc = "Speed effect level.")
    public int configSpeedLevel;

    @ConfigLong(def = 2000, min = 50, max = 5000, desc = "Speed effect duration in milliseconds.")
    public long configDuration;

    @ConfigMaterialSet(def = { Material.DIRT_PATH }, desc = "Materials on which players will walk faster.")
    public Set<Material> configMaterials;

    // Variables
    public PotionEffect walkSpeedEffect;

    public FastWalkingGroup(Context<Trifles> context) {
        super(context, "FastWalking", "Enable faster walking on certain materials.");
    }

    @Override
    public void onConfigChange() {
        var ticks = msToTicks(configDuration);
        walkSpeedEffect = new PotionEffect(PotionEffectType.SPEED, (int) ticks, configSpeedLevel - 1)
            .withAmbient(false)
            .withParticles(false)
            .withIcon(false);
    }
}
