package org.oddlama.vane.annotation.config

/** Map from string keys to lists of strings configuration entries. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigStringListMap(
    val def: Array<ConfigStringListMapEntry>,
    val desc: String,
    val metrics: Boolean = false
)
