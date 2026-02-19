package org.oddlama.vane.annotation.enchantment

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class VaneEnchantment(
    val name: String,
    val maxLevel: Int = 1,
    val rarity: Rarity = Rarity.COMMON,
    val curse: Boolean = false,
    val tradeable: Boolean = false,
    val treasure: Boolean = false,
    val generateInTreasure: Boolean = false,
    val allowCustom: Boolean = false
)
