package org.oddlama.vane.annotation.enchantment

/** Rarity levels used for enchantment generation and loot weight. */
enum class Rarity {
    /** Common: high chance to appear. */
    COMMON,

    /** Uncommon: lower chance than common. */
    UNCOMMON,

    /** Rare: uncommon but more common than very rare. */
    RARE,

    /** Very rare: minimal chance to appear. */
    VERY_RARE,
}
