package org.oddlama.vane.util

object ArrayUtil {
    @JvmStatic
    fun <T> prepend(arr: Array<T?>, element: T?): Array<T?> {
        var arr = arr
        val n = arr.size
        arr = arr.copyOf(n + 1)
        for (i in arr.size - 1 downTo 1) {
            arr[i] = arr[i - 1]
        }
        arr[0] = element
        return arr
    }

    fun <T> append(arr: Array<T?>, element: T?): Array<T?> {
        var arr = arr
        val n = arr.size
        arr = arr.copyOf(n + 1)
        arr[n] = element
        return arr
    }
}
