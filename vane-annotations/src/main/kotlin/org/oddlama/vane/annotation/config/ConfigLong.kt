package org.oddlama.vane.annotation.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigLong(
    val def: Long,
    val min: Long = Long.MIN_VALUE,
    val max: Long = Long.MAX_VALUE,
    val desc: String,
    val metrics: Boolean = true
)
