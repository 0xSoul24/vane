package org.oddlama.vane.core.config

interface ConfigDictSerializable {
    fun toDict(): MutableMap<String?, Any?>?

    fun fromDict(dict: MutableMap<String?, Any?>?)
}
