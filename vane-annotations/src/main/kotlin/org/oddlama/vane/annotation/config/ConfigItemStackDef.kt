package org.oddlama.vane.annotation.config

import org.bukkit.Material

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigItemStackDef(val type: Material, val amount: Int = 1)
