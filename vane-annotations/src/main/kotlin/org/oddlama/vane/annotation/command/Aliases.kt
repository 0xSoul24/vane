package org.oddlama.vane.annotation.command

/**
 * Additional alias names for a command.
 *
 * @property value Alternative names that can invoke the same command.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Aliases(vararg val value: String)
