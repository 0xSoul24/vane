package org.oddlama.vane.annotation.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigDoubleList(
    val def: DoubleArray,
    val min: Double = Double.NaN,
    val max: Double = Double.NaN,
    val desc: String,
    val metrics: Boolean = true
)
