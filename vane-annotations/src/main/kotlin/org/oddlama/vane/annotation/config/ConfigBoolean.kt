package org.oddlama.vane.annotation.config

/** Boolean configuration field. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigBoolean(val def: Boolean, val desc: String, val metrics: Boolean = true)
