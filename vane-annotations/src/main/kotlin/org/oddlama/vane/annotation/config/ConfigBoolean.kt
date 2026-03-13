package org.oddlama.vane.annotation.config

/**
 * Boolean configuration field.
 *
 * @property def Default value used when not configured.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigBoolean(val def: Boolean, val desc: String, val metrics: Boolean = true)
