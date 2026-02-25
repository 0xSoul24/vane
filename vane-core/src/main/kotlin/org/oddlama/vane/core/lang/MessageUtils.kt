package org.oddlama.vane.core.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Utility functions used by translated message classes to avoid duplicating
 * argument serialization logic.
 */
fun stringifyArgsForStr(key: String?, args: Array<out Any?>): Array<Any?> {
    val argsAsStrings = arrayOfNulls<Any>(args.size)
    for (i in args.indices) {
        when (val arg = args[i]) {
            is Component -> {
                argsAsStrings[i] = LegacyComponentSerializer.legacySection().serialize(arg)
            }

            is String -> {
                argsAsStrings[i] = arg
            }

            else -> {
                throw RuntimeException("Error while formatting message '$key', invalid argument to str() serializer: $arg")
            }
        }
    }
    return argsAsStrings
}

