package org.oddlama.vane.annotation.config

/** Double configuration field with optional bounds. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigDouble(
    val def: Double,
    val min: Double = Double.NaN,
    val max: Double = Double.NaN,
    val desc: String,
    val metrics: Boolean = true
)
