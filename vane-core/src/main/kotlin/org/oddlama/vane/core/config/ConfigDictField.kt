package org.oddlama.vane.core.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigDict
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException

class ConfigDictField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    var annotation: ConfigDict
) : ConfigField<ConfigDictSerializable?>(owner, field, mapName, "dict", annotation.desc) {

    private class EmptyDict : ConfigDictSerializable {
        override fun toDict(): MutableMap<String, Any> = mutableMapOf()
        override fun fromDict(dict: MutableMap<String, Any>) = Unit
    }

    override fun def(): ConfigDictSerializable = overriddenDef() ?: EmptyDict()
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    private fun appendList(builder: StringBuilder, indent: String?, listKey: String?, list: MutableList<Any?>) {
        builder.append("$indent$listKey")
        if (list.isEmpty()) {
            builder.append(": []\n")
        } else {
            builder.append(":\n")
            list.forEach { entry ->
                when (entry) {
                    is String -> builder.append("$indent  - \"${escapeYaml(entry)}\"\n")
                    is Int, is Long, is Float, is Double, is Boolean -> builder.append("$indent  - $entry\n")
                    is MutableMap<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        appendDict(builder, "$indent  ", null, entry as MutableMap<String, Any>, true)
                    }
                    else -> throw RuntimeException("Invalid value '$entry' of type ${entry!!.javaClass} in mapping of ConfigDictSerializable")
                }
            }
        }
    }

    private fun appendDict(builder: StringBuilder, indent: String?, dictKey: String?, dict: MutableMap<String, Any>, isListEntry: Boolean) {
        builder.append(indent)
        if (isListEntry) builder.append("-") else builder.append("$dictKey:")
        if (dict.isEmpty()) {
            builder.append(" {}\n")
        } else {
            builder.append("\n")
            dict.entries.sortedBy { it.key }.forEach { (k, v) ->
                when (v) {
                    is String -> builder.append("$indent  $k: \"${escapeYaml(v)}\"\n")
                    is Int, is Long, is Float, is Double, is Boolean -> builder.append("$indent  $k: $v\n")
                    is MutableMap<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        appendDict(builder, "$indent  ", k, v as MutableMap<String, Any>, false)
                    }
                    is MutableList<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        appendList(builder, "$indent  ", k, v as MutableList<Any?>)
                    }
                    else -> throw RuntimeException("Invalid value '$v' of type ${v.javaClass} in mapping of ConfigDictSerializable")
                }
            }
        }
    }

    private fun appendDict(builder: StringBuilder, indent: String?, defaultDefinition: Boolean, ser: ConfigDictSerializable) {
        if (defaultDefinition) appendDict(builder, "$indent# ", "Default", ser.toDict(), false)
        else appendDict(builder, indent, basename(), ser.toDict(), false)
    }

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        appendDict(builder, indent, true, def())
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig) else def()
        appendDict(builder, indent, false, def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isConfigurationSection(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected configuration section")
    }

    fun loadListFromYaml(rawList: MutableList<*>): MutableList<Any?> =
        rawList.mapTo(mutableListOf()) { e ->
            if (e is ConfigurationSection) loadDictFromYaml(e) else e
        }

    fun loadDictFromYaml(section: ConfigurationSection): MutableMap<String, Any> =
        section.getKeys(false).associateWithTo(mutableMapOf()) { subkey ->
            when {
                section.isConfigurationSection(subkey) -> loadDictFromYaml(section.getConfigurationSection(subkey)!!)
                section.isList(subkey)    -> loadListFromYaml(section.getList(subkey)!!)
                section.isString(subkey)  -> section.getString(subkey)!!
                section.isInt(subkey)     -> section.getInt(subkey)
                section.isDouble(subkey)  -> section.getDouble(subkey)
                section.isBoolean(subkey) -> section.getBoolean(subkey)
                section.isLong(subkey)    -> section.getLong(subkey)
                else -> throw IllegalStateException("Cannot load dict entry '${yamlPath()}.$subkey': unknown type")
            }
        }

    fun loadFromYaml(yaml: YamlConfiguration): ConfigDictSerializable {
        try {
            val dict = annotation.cls.java.getDeclaredConstructor().newInstance() as ConfigDictSerializable
            dict.fromDict(loadDictFromYaml(yaml.getConfigurationSection(yamlPath())!!))
            return dict
        } catch (e: Exception) {
            when (e) {
                is InstantiationException, is IllegalAccessException, is IllegalArgumentException,
                is InvocationTargetException, is NoSuchMethodException, is SecurityException ->
                    throw RuntimeException("Could not instanciate storage class for ConfigDict: ${annotation.cls}", e)
                else -> throw e
            }
        }
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
