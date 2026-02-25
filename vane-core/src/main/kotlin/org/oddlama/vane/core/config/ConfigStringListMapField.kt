package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigStringListMap
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field
import java.util.*
import java.util.function.Consumer
import java.util.function.Function

class ConfigStringListMapField(
    owner: Any?,
    field: Field,
    mapName: Function<String?, String?>,
    var annotation: ConfigStringListMap
) : ConfigField<MutableMap<String?, MutableList<String?>?>?>(
    owner,
    field,
    mapName,
    "map of string to string list",
    annotation.desc
) {
    private fun appendStringListMapDefinition(
        builder: StringBuilder,
        indent: String?,
        prefix: String?,
        def: MutableMap<String?, MutableList<String?>?>
    ) {
        def.forEach { (k: String?, list: MutableList<String?>?) ->
            builder.append(indent)
            builder.append(prefix)
            builder.append("  ")
            // Use PascalCase for keys in generated YAML
            builder.append(escapeYaml(toPascalCase(k)))
            builder.append(":\n")
            list!!.forEach(Consumer { s: String? ->
                builder.append(indent)
                builder.append(prefix)
                builder.append("    - ")
                builder.append(escapeYaml(s))
                builder.append("\n")
            })
        }
    }

    override fun def(): MutableMap<String?, MutableList<String?>?> {
        val override = overriddenDef()
        return override
            ?: annotation.def.associate { e -> e.key to e.list.toMutableList<String?>() }.toMutableMap()
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
        appendStringListMapDefinition(builder, indent, "# ", def())

        // Definition
        builder.append(indent)
        builder.append(basename())
        builder.append(":\n")
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig)
        else
            def()
        appendStringListMapDefinition(builder, indent, "", def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)

        if (!yaml.isConfigurationSection(yamlPath())) {
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected group")
        }

        for (listKey in yaml.getConfigurationSection(yamlPath())!!.getKeys(false)) {
            val listPath = yamlPath() + "." + listKey
            if (!yaml.isList(listPath)) {
                throw YamlLoadException("Invalid type for yaml path '$listPath', expected list")
            }

            for (obj in yaml.getList(listPath)!!) {
                if (obj !is String) {
                    throw YamlLoadException("Invalid type for yaml path '$listPath', expected string")
                }
            }
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): MutableMap<String?, MutableList<String?>?> {
        val map = HashMap<String?, MutableList<String?>?>()
        for (listKey in yaml.getConfigurationSection(yamlPath())!!.getKeys(false)) {
            val listPath = yamlPath() + "." + listKey
            val list = ArrayList<String?>()
            // Normalize keys so in-memory representation stays lowercase/underscore
            map[normalizeKey(listKey)] = list
            for (obj in yaml.getList(listPath)!!) {
                list.add(obj as String?)
            }
        }
        return map
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }

    companion object {
        // Convert a key like "default" or "terralith_rare" into PascalCase: "Default" / "TerralithRare"
        private fun toPascalCase(key: String?): String? {
            if (key.isNullOrEmpty()) return key
            val parts: Array<String?> =
                key.split("[^A-Za-z0-9]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val sb = StringBuilder()
            for (p in parts) {
                if (p!!.isEmpty()) continue
                sb.append(p[0].uppercaseChar())
                if (p.length > 1) sb.append(p.substring(1))
            }
            return sb.toString()
        }

        // Normalize keys from YAML back to canonical internal form (lowercase, non-alphanum -> underscore)
        private fun normalizeKey(key: String?): String? {
            if (key == null) return null
            return key.lowercase(Locale.getDefault()).replace("[^a-z0-9]+".toRegex(), "_")
        }
    }
}
