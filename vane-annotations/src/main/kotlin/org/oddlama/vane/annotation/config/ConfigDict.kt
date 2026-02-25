package org.oddlama.vane.annotation.config

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigDict(val cls: KClass<*>, val desc: String, val metrics: Boolean = false)
