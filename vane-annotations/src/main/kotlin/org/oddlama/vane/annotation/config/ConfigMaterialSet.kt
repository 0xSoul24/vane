package org.oddlama.vane.annotation.config

import org.bukkit.Material

/** Set of Materials used for configuration purposes. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialSet(val def: Array<Material>, val desc: String, val metrics: Boolean = true)
