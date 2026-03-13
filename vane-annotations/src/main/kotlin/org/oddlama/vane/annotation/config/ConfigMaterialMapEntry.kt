package org.oddlama.vane.annotation.config

import org.bukkit.Material

/**
 * Entry mapping a string key to a single Material.
 *
 * @property key Top-level key in the map.
 * @property value Material mapped to [key].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialMapEntry(val key: String, val value: Material)
