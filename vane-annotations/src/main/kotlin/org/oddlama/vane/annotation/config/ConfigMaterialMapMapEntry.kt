package org.oddlama.vane.annotation.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialMapMapEntry(val key: String, vararg val value: ConfigMaterialMapEntry)
