package org.oddlama.vane.annotation.command

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Aliases(vararg val value: String)
