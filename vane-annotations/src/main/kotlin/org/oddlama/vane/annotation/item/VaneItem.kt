package org.oddlama.vane.annotation.item

import org.bukkit.Material

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class VaneItem(
    val name: String,
    val base: Material,
    val modelData: Int,
    val version: Int,
    val durability: Int = 0,
    val enabled: Boolean = true
)
