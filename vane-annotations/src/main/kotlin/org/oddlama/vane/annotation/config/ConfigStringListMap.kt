package org.oddlama.vane.annotation.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigStringListMap(
    val def: Array<ConfigStringListMapEntry>,
    val desc: String,
    val metrics: Boolean = false
)
