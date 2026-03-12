package org.oddlama.vane.annotation.config

import org.bukkit.Material

/** Entry mapping a string key to a single Material. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialMapEntry(val key: String, val value: Material)
