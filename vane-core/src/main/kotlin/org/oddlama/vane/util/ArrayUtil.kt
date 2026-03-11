package org.oddlama.vane.util

fun <T> Array<T?>.prepend(element: T?): Array<T?> =
    copyOf(size + 1).also {
        System.arraycopy(this, 0, it, 1, size)
        it[0] = element
    }

fun <T> Array<T?>.append(element: T?): Array<T?> =
    copyOf(size + 1).also { it[size] = element }
