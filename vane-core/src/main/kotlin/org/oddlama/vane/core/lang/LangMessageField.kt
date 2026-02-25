package org.oddlama.vane.core.lang

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import java.lang.reflect.Field
import java.util.function.Function

class LangMessageField(
    module: Module<*>,
    owner: Any?,
    field: Field,
    mapName: Function<String?, String>,
    var annotation: LangMessage?
) : LangField<TranslatedMessage?>(module, owner, field, mapName) {
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration?) {
        if (yaml == null) throw YamlLoadException.Lang("yaml is null", this)
        checkYamlPath(yaml)

        if (!yaml.isString(yamlPath())) {
            throw YamlLoadException.Lang("Invalid type for yaml path '" + yamlPath() + "', expected string", this)
        }
    }

    private fun fromYaml(yaml: YamlConfiguration): String {
        return yaml.getString(yamlPath()) ?: ""
    }

    override fun load(namespace: String?, yaml: YamlConfiguration?) {
        if (yaml == null) throw YamlLoadException.Lang("yaml is null", this)
        try {
            field.set(owner, TranslatedMessage(module(), key(), fromYaml(yaml)))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }

    @Throws(YamlLoadException::class)
    override fun addTranslations(pack: ResourcePackGenerator?, yaml: YamlConfiguration?, langCode: String?) {
        if (pack == null || yaml == null || langCode == null) return
        checkLoadable(yaml)
        pack.translations(namespace(), langCode).put(key(), fromYaml(yaml))
    }
}
