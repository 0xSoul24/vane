package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigStringList
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field
import java.util.*
import java.util.function.Function

class ConfigStringListField(
    owner: Any?,
    field: Field,
    mapName: Function<String?, String?>,
    var annotation: ConfigStringList
) : ConfigField<MutableList<String?>?>(owner, field, mapName, "list of strings", annotation.desc) {
    private fun appendStringListDefinition(
        builder: StringBuilder?,
        indent: String?,
        prefix: String?,
        def: MutableList<String?>
    ) {
        appendListDefinition<String?>(builder, indent, prefix, def) { b: StringBuilder?, s: String? ->
            b!!.append("\"")
            b.append(escapeYaml(s))
            b.append("\"")
        }
    }

    override fun def(): MutableList<String?> {
        val override = overriddenDef()
        if (override != null) return override
        val list = ArrayList<String?>()
        for (s in annotation.def) {
            list.add(s)
        }
        return list
    }

    override fun metrics(): Boolean {
        val override = overriddenMetrics()
        return override ?: annotation.metrics
    }

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)

        // Default
        builder.append(indent)
        builder.append("# Default:\n")
        appendStringListDefinition(builder, indent, "# ", def())

        // Definition
        builder.append(indent)
        builder.append(basename())
        builder.append(":\n")
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig)
        else
            def()
        appendStringListDefinition(builder, indent, "", def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)

        if (!yaml.isList(yamlPath())) {
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected list")
        }

        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is String) {
                throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected string")
            }
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): MutableList<String?> {
        val list = ArrayList<String?>()
        for (obj in yaml.getList(yamlPath())!!) {
            list.add(obj as String?)
        }
        return list
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }
}
