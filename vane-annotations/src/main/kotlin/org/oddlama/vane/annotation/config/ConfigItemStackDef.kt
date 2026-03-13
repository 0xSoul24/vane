package org.oddlama.vane.annotation.config

import org.bukkit.Material

/**
 * Defines a default ItemStack for `@ConfigItemStack` fields.
 *
 * @property type Material used for the default item stack.
 * @property amount Item amount for the default item stack.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigItemStackDef(val type: Material, val amount: Int = 1)
