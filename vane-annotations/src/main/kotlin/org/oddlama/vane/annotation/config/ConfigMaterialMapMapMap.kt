package org.oddlama.vane.annotation.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialMapMapMap(
    val def: Array<ConfigMaterialMapMapMapEntry>,
    val desc: String,
    val metrics: Boolean = false
)
