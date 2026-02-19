package org.oddlama.vane.annotation.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigInt(
    val def: Int,
    val min: Int = Int.MIN_VALUE,
    val max: Int = Int.MAX_VALUE,
    val desc: String,
    val metrics: Boolean = true
)
