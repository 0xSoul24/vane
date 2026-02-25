package org.oddlama.vane.annotation.config

import org.bukkit.Material

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialSet(val def: Array<Material>, val desc: String, val metrics: Boolean = true)
