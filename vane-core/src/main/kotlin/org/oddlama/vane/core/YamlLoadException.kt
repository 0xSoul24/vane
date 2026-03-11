package org.oddlama.vane.core

import org.oddlama.vane.core.lang.LangField

open class YamlLoadException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {
    class Lang(message: String?, val langField: LangField<*>) : YamlLoadException(message) {
        override val message: String
            get() = "[${langField}] ${super.message}"
    }
}
