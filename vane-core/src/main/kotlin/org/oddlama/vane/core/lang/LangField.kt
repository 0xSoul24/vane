package org.oddlama.vane.core.lang

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import java.lang.reflect.Field
import java.util.function.Function

abstract class LangField<T>(private val module: Module<*>, protected var owner: Any?,
                            protected var field: Field, mapName: Function<String?, String>) {
    var name: String
        protected set
    private val namespace: String
    private val key: String

    init {

        if (!field.name.startsWith(PREFIX)) throw RuntimeException(
            YamlLoadException.Lang("field must start with $PREFIX", this)
        )
        this.name = mapName.apply(field.name.substring(PREFIX.length))
        this.namespace = module.namespace()
        this.key = namespace + "." + yamlPath()

        field.setAccessible(true)
    }

    fun yamlPath(): String {
        return name
    }

    @Throws(YamlLoadException::class)
    protected fun checkYamlPath(yaml: YamlConfiguration) {
        if (!yaml.contains(name, true)) {
            throw YamlLoadException.Lang("yaml is missing entry with path '$name'", this)
        }
    }

    fun module(): Module<*> {
        return module
    }

    fun namespace(): String {
        return namespace
    }

    fun key(): String {
        return key
    }

    @Throws(YamlLoadException::class)
    abstract fun checkLoadable(yaml: YamlConfiguration?)

    abstract fun load(namespace: String?, yaml: YamlConfiguration?)

    @Throws(YamlLoadException::class)
    abstract fun addTranslations(
        pack: ResourcePackGenerator?,
        yaml: YamlConfiguration?,
        langCode: String?
    )

    @Suppress("UNCHECKED_CAST")
    fun get(): T? {
        try {
            return field.get(owner) as T?
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }

    override fun toString(): String {
        return (field.declaringClass.getTypeName() + "::" + field.name)
    }

    companion object {
        const val PREFIX: String = "lang"
    }
}
