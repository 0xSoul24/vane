package org.oddlama.vane.annotation.config

/**
 * Entry for a map of string keys to lists of strings.
 *
 * @property key Map key.
 * @property list List of string values mapped to [key].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigStringListMapEntry(val key: String, val list: Array<String>)
