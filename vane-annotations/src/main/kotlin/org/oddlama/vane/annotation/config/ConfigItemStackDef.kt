package org.oddlama.vane.annotation.config

import org.bukkit.Material

/** Defines a default ItemStack for `@ConfigItemStack` fields. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigItemStackDef(val type: Material, val amount: Int = 1)
