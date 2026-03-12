package org.oddlama.vane.annotation.command

/** Specifies the primary name for a command class. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Name(val value: String)
