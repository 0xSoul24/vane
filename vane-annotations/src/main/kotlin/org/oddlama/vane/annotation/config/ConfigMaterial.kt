package org.oddlama.vane.annotation.config

import org.bukkit.Material

/**
 * Material configuration field using a Bukkit Material.
 *
 * @property def Default material value.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterial(val def: Material, val desc: String, val metrics: Boolean = true)
