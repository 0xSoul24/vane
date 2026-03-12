package org.oddlama.vane.annotation.config

/** ItemStack configuration field with a default defined by [ConfigItemStackDef]. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigItemStack(val def: ConfigItemStackDef, val desc: String, val metrics: Boolean = true)
