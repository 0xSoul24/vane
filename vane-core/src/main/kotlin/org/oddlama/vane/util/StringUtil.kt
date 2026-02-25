package org.oddlama.vane.util

/**
 * Utility functions for string transformations.
 */

fun snakeCaseToPascalCase(snakeCase: String): String {
    val result = StringBuilder()
    var capitalizeNext = true

    for (c in snakeCase.toCharArray()) {
        if (c == '_') {
            capitalizeNext = true
        } else if (capitalizeNext) {
            result.append(c.uppercaseChar())
            capitalizeNext = false
        } else {
            result.append(c)
        }
    }

    return result.toString()
}

