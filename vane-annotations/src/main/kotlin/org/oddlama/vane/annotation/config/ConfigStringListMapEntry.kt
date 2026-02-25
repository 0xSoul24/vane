package org.oddlama.vane.annotation.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigStringListMapEntry(val key: String, val list: Array<String>)
