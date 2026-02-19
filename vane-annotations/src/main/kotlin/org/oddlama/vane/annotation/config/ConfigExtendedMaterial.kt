package org.oddlama.vane.annotation.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigExtendedMaterial(val def: String, val desc: String, val metrics: Boolean = true)
