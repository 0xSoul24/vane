package org.oddlama.vane.annotation.config

import kotlin.reflect.KClass

/**
 * Dictionary/map configuration field where values are instances of the given class.
 *
 * @property cls Value class type stored in the dictionary.
 * @property desc Description for documentation/config generation.
 * @property metrics Whether to include this field in metrics.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigDict(val cls: KClass<*>, val desc: String, val metrics: Boolean = false)
