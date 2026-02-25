package org.oddlama.vane.core.lang

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.lang.LangVersion
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import java.lang.reflect.Field
import java.util.function.Function

class LangVersionField(
    module: Module<*>,
    owner: Any?,
    field: Field,
    mapName: Function<String?, String>,
    var annotation: LangVersion?
) : LangField<Long?>(module, owner, field, mapName) {
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration?) {
        if (yaml == null) throw YamlLoadException.Lang("yaml is null", this)
        checkYamlPath(yaml)

        if (yaml.get(yamlPath()) !is Number) {
            throw YamlLoadException.Lang("Invalid type for yaml path '" + yamlPath() + "', expected long", this)
        }

        val `val` = yaml.getLong(yamlPath())
        if (`val` < 1) {
            throw YamlLoadException.Lang(
                "Entry '" + yamlPath() + "' has an invalid value: Value must be >= 1",
                this
            )
        }
    }

    override fun load(namespace: String?, yaml: YamlConfiguration?) {
        if (yaml == null) throw YamlLoadException.Lang("yaml is null", this)
        try {
            field.setLong(owner, yaml.getLong(yamlPath()))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }

    @Throws(YamlLoadException::class)
    override fun addTranslations(pack: ResourcePackGenerator?, yaml: YamlConfiguration?, langCode: String?) {}
}
