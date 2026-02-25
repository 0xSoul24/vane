package org.oddlama.vane.annotation.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialMapMapMapEntry(val key: String, vararg val value: ConfigMaterialMapMapEntry)
