package org.oddlama.vane.core.lang

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.lang.LangMessageArray
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import java.lang.reflect.Field
import java.util.function.Function

class LangMessageArrayField(
    module: Module<*>,
    owner: Any?,
    field: Field,
    mapName: Function<String?, String>,
    var annotation: LangMessageArray?
) : LangField<TranslatedMessageArray?>(module, owner, field, mapName) {
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration?) {
        if (yaml == null) throw YamlLoadException.Lang("yaml is null", this)
        checkYamlPath(yaml)

        if (!yaml.isList(yamlPath())) {
            throw YamlLoadException.Lang("Invalid type for yaml path '" + yamlPath() + "', expected list", this)
        }

        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is String) {
                throw YamlLoadException.Lang(
                    "Invalid type for yaml path '" + yamlPath() + "', expected string",
                    this
                )
            }
        }
    }

    private fun fromYaml(yaml: YamlConfiguration): MutableList<String> {
        val list = ArrayList<String>()
        for (obj in yaml.getList(yamlPath())!!) {
            list.add(obj as String)
        }
        return list
    }

    override fun load(namespace: String?, yaml: YamlConfiguration?) {
        if (yaml == null) throw YamlLoadException.Lang("yaml is null", this)
        try {
            field.set(owner, TranslatedMessageArray(module(), key(), fromYaml(yaml)))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }

    @Throws(YamlLoadException::class)
    override fun addTranslations(pack: ResourcePackGenerator?, yaml: YamlConfiguration?, langCode: String?) {
        if (pack == null || yaml == null || langCode == null) return
        checkLoadable(yaml)
        val list = fromYaml(yaml)
        val loadedSize = get()!!.size()
        if (list.size != loadedSize) {
            throw YamlLoadException.Lang(
                "All translation lists for message arrays must have the exact same size. The loaded language file has " +
                        loadedSize +
                        " entries, while the currently processed file has " +
                        list.size,
                this
            )
        }
        for (i in list.indices) {
            pack.translations(namespace(), langCode).put(key() + "." + i, list[i])
        }
    }
}
