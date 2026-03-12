package org.oddlama.vane.annotation.config

/** Nested map configuration where the innermost values are Materials. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialMapMapMap(
    val def: Array<ConfigMaterialMapMapMapEntry>,
    val desc: String,
    val metrics: Boolean = false
)
