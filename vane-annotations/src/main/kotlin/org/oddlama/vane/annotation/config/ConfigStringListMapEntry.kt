package org.oddlama.vane.annotation.config

/** Entry for a map of string keys to lists of strings. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigStringListMapEntry(val key: String, val list: Array<String>)
