package org.oddlama.vane.annotation.command

import java.lang.annotation.Inherited

/** Marks a class as a Vane command for source-level processing. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@Inherited
annotation class VaneCommand 
