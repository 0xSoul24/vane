package org.oddlama.vane.annotation.config

import org.bukkit.Material

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialMapEntry(val key: String, val value: Material)
