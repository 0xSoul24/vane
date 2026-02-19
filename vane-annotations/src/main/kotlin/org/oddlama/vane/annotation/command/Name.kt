package org.oddlama.vane.annotation.command

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Name(val value: String)
