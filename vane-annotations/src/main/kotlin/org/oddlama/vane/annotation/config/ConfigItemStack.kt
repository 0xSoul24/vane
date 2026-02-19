package org.oddlama.vane.annotation.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigItemStack(val def: ConfigItemStackDef, val desc: String, val metrics: Boolean = true)
