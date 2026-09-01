package org.oddlama.vane.annotation.config

/**
 * Entry for a map of string keys to lists of strings.
 *
 * @property key Map key.
 * @property list List of string values mapped to [key].
 */
@Retention(AnnotationRetention.RUNTIME)
// Nested value only: this annotation is written inside [ConfigStringListMap]'s `def`, never on a
// field. An empty target list is what says so, and it also keeps `ConfigManager`'s
// `Config*` field scan from ever picking it up as a config field of its own.
@Target()
annotation class ConfigStringListMapEntry(val key: String, val list: Array<String>)
