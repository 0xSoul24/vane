package org.oddlama.vane.annotation.config

/** Extended material configuration using a string identifier for custom materials. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigExtendedMaterial(val def: String, val desc: String, val metrics: Boolean = true)
