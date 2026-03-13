package org.oddlama.vane.annotation.config

/**
 * Entry for a two-level nested map structure mapping keys to nested material maps.
 *
 * @property key Top-level key in the nested map.
 * @property value Nested map entries under [key].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialMapMapMapEntry(val key: String, vararg val value: ConfigMaterialMapMapEntry)
