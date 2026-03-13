package org.oddlama.vane.annotation.config

/**
 * Entry for a map from string keys to maps of materials (one nesting level).
 *
 * @property key Top-level key in the nested map.
 * @property value Nested material map entries under [key].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialMapMapEntry(val key: String, vararg val value: ConfigMaterialMapEntry)
