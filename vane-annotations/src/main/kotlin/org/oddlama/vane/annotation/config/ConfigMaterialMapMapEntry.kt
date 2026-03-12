package org.oddlama.vane.annotation.config

/** Entry for a map from string keys to maps of materials (one nesting level). */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialMapMapEntry(val key: String, vararg val value: ConfigMaterialMapEntry)
