package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigStringListMap
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field
import java.util.Locale

class ConfigStringListMapField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    var annotation: ConfigStringListMap
) : ConfigField<MutableMap<String?, MutableList<String?>?>?>(
    owner, field, mapName, "map of string to string list", annotation.desc
) {
    private fun appendStringListMapDefinition(builder: StringBuilder, indent: String?, prefix: String?, def: MutableMap<String?, MutableList<String?>?>) {
        def.forEach { (k, list) ->
            builder.append("$indent$prefix  ${escapeYaml(toPascalCase(k))}:\n")
            list!!.forEach { s -> builder.append("$indent$prefix    - ${escapeYaml(s)}\n") }
        }
    }

    override fun def(): MutableMap<String?, MutableList<String?>?> =
        overriddenDef() ?: annotation.def.associate { e -> e.key to e.list.toMutableList<String?>() }.toMutableMap()

    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        builder.append("$indent# Default:\n")
        appendStringListMapDefinition(builder, indent, "# ", def())
        builder.append("$indent${basename()}:\n")
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig) else def()
        appendStringListMapDefinition(builder, indent, "", def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isConfigurationSection(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected group")
        for (listKey in yaml.getConfigurationSection(yamlPath())!!.getKeys(false)) {
            val listPath = "${yamlPath()}.$listKey"
            if (!yaml.isList(listPath))
                throw YamlLoadException("Invalid type for yaml path '$listPath', expected list")
            for (obj in yaml.getList(listPath)!!) {
                if (obj !is String)
                    throw YamlLoadException("Invalid type for yaml path '$listPath', expected string")
            }
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): MutableMap<String?, MutableList<String?>?> =
        yaml.getConfigurationSection(yamlPath())!!.getKeys(false).associateTo(mutableMapOf()) { listKey ->
            val listPath = "${yamlPath()}.$listKey"
            normalizeKey(listKey) to yaml.getList(listPath)!!.map { it as String? }.toMutableList()
        }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }

    companion object {
        private fun toPascalCase(key: String?): String? {
            if (key.isNullOrEmpty()) return key
            return key.split("[^A-Za-z0-9]+".toRegex())
                .filter { it.isNotEmpty() }
                .joinToString("") { it[0].uppercaseChar() + it.substring(1) }
        }

        private fun normalizeKey(key: String?): String? =
            key?.lowercase(Locale.getDefault())?.replace("[^a-z0-9]+".toRegex(), "_")
    }
}
