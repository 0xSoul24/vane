package org.oddlama.vane.annotation.config

/** Entry for a two-level nested map structure mapping keys to nested material maps. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialMapMapMapEntry(val key: String, vararg val value: ConfigMaterialMapMapEntry)
