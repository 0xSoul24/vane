package org.oddlama.vane.annotation.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigStringList(val def: Array<String>, val desc: String, val metrics: Boolean = false)
