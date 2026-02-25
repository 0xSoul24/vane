package org.oddlama.vane.core

import org.oddlama.vane.core.lang.LangField

open class YamlLoadException : Exception {
    constructor(message: String?) : super(message)

    constructor() : super()

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable?) : super(cause)

    protected constructor(
        message: String?,
        cause: Throwable?,
        enableSuppression: Boolean,
        writableStackTrace: Boolean
    ) : super(message, cause, enableSuppression, writableStackTrace)

    class Lang(message: String?, val langField: LangField<*>) : YamlLoadException(message) {
        override val message: String
            get() = "[" + this.langField.toString() + "] " + super.message
    }
}
