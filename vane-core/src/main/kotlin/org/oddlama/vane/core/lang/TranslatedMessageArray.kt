package org.oddlama.vane.core.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.oddlama.vane.core.module.Module
import java.util.stream.Collectors

class TranslatedMessageArray(
    private val module: Module<*>,
    private val key: String?,
    private val defaultTranslation: MutableList<String>
) {
    fun size(): Int {
        return defaultTranslation.size
    }

    fun key(): String? {
        return key
    }

    fun str(vararg args: Any?): MutableList<String?> {
        try {
            val argsAsStrings = stringifyArgsForStr(key, args)

            val list = ArrayList<String?>()
            for (s in defaultTranslation) {
                list.add(String.format(s, *argsAsStrings))
            }
            return list
        } catch (e: Exception) {
            throw RuntimeException("Error while formatting message '" + key() + "'", e)
        }
    }

    fun format(vararg args: Any?): MutableList<Component?> {
        if (module.core?.configClientSideTranslations != true) {
            return str(*args)
                .stream()
                .map { s: String? -> LegacyComponentSerializer.legacySection().deserialize(s!!) }
                .collect(Collectors.toList())
        }

        val arr = ArrayList<Component?>()
        for (i in defaultTranslation.indices) {
            val list = ArrayList<ComponentLike?>()
            for (o in args) {
                when (o) {
                    is ComponentLike -> {
                        list.add(o)
                    }

                    is String -> {
                        list.add(LegacyComponentSerializer.legacySection().deserialize(o))
                    }

                    else -> {
                        throw RuntimeException(
                            "Error while formatting message '" + key() + "', got invalid argument " + o
                        )
                    }
                }
            }
            arr.add(Component.translatable("$key.$i", list))
        }
        return arr
    }
}
