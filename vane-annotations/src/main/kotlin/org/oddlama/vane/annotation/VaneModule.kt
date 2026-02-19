package org.oddlama.vane.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class VaneModule(
    val name: String,
    val bstats: Int = -1,
    val configVersion: Long,
    val langVersion: Long,
    val storageVersion: Long
)
