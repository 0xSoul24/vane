package org.oddlama.vane.annotation.config

/** List of strings configuration field. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigStringList(val def: Array<String>, val desc: String, val metrics: Boolean = false)
