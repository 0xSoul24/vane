package org.oddlama.vane.core.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.oddlama.vane.core.module.Module

class TranslatedMessageArray(
    private val module: Module<*>,
    private val key: String?,
    private val defaultTranslation: MutableList<String>
) {
    fun size(): Int = defaultTranslation.size
    fun key(): String? = key

    fun str(vararg args: Any?): MutableList<String?> =
        try {
            val argsAsStrings = stringifyArgsForStr(key, args)
            defaultTranslation.mapTo(mutableListOf()) { s -> String.format(s, *argsAsStrings) }
        } catch (e: Exception) {
            throw RuntimeException("Error while formatting message '${key()}'", e)
        }

    fun format(vararg args: Any?): MutableList<Component?> {
        if (module.core?.configClientSideTranslations != true) {
            return str(*args).mapTo(mutableListOf()) { s ->
                LegacyComponentSerializer.legacySection().deserialize(s!!)
            }
        }

        return defaultTranslation.indices.mapTo(mutableListOf()) { i ->
            val list = args.map { o ->
                when (o) {
                    is ComponentLike -> o
                    is String -> LegacyComponentSerializer.legacySection().deserialize(o)
                    else -> throw RuntimeException("Error while formatting message '${key()}', got invalid argument $o")
                }
            }
            Component.translatable("$key.$i", list)
        }
    }
}
