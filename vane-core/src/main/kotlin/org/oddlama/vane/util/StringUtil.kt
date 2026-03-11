package org.oddlama.vane.util

/**
 * Utility functions for string transformations.
 */

fun snakeCaseToPascalCase(snakeCase: String): String =
    snakeCase.split('_').joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
