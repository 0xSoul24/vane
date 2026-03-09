package org.oddlama.vane.trifles

import org.bukkit.Material
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.annotation.config.ConfigMaterialSet
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleGroup
import org.oddlama.vane.util.Conversions

class FastWalkingGroup(context: Context<Trifles?>) :
    ModuleGroup<Trifles?>(context, "FastWalking", "Enable faster walking on certain materials.") {
    @ConfigInt(def = 2, min = 1, max = 10, desc = "Speed effect level.")
    var configSpeedLevel: Int = 0

    @ConfigLong(def = 2000, min = 50, max = 5000, desc = "Speed effect duration in milliseconds.")
    var configDuration: Long = 0

    @JvmField
    @ConfigMaterialSet(def = [Material.DIRT_PATH], desc = "Materials on which players will walk faster.")
    var configMaterials: MutableSet<Material?>? = null

    // Variables
    @JvmField
    var walkSpeedEffect: PotionEffect? = null

    override fun onConfigChange() {
        val ticks = Conversions.msToTicks(configDuration)
        walkSpeedEffect = PotionEffect(PotionEffectType.SPEED, ticks.toInt(), configSpeedLevel - 1)
            .withAmbient(false)
            .withParticles(false)
            .withIcon(false)
    }
}
