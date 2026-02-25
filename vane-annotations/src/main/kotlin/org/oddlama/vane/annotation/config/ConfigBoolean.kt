package org.oddlama.vane.annotation.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigBoolean(val def: Boolean, val desc: String, val metrics: Boolean = true)
