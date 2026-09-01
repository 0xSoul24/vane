package org.oddlama.vane.annotation.config

import org.bukkit.Material

/**
 * Defines a default ItemStack for `@ConfigItemStack` fields.
 *
 * @property type Material used for the default item stack.
 * @property amount Item amount for the default item stack.
 */
@Retention(AnnotationRetention.RUNTIME)
// Nested value only: this annotation is written inside [ConfigItemStack]'s `def`, never on a
// field. An empty target list is what says so, and it also keeps `ConfigManager`'s
// `Config*` field scan from ever picking it up as a config field of its own.
@Target()
annotation class ConfigItemStackDef(val type: Material, val amount: Int = 1)
