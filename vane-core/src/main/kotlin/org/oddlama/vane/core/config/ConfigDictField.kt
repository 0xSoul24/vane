package org.oddlama.vane.core.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigDict
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.util.function.Consumer
import java.util.function.Function

class ConfigDictField(
    owner: Any?,
    field: Field,
    mapName: Function<String?, String?>,
    var annotation: ConfigDict
) : ConfigField<ConfigDictSerializable?>(owner, field, mapName, "dict", annotation.desc) {
    private class EmptyDict : ConfigDictSerializable {
        override fun toDict(): MutableMap<String?, Any?> {
            return HashMap()
        }

        override fun fromDict(dict: MutableMap<String?, Any?>?) {
            // no-op
        }
    }

    override fun def(): ConfigDictSerializable {
        val override = overriddenDef()
        return override ?: EmptyDict()
    }

    override fun metrics(): Boolean {
        val override = overriddenMetrics()
        return override ?: annotation.metrics
    }

    private fun appendList(
        builder: StringBuilder,
        indent: String?,
        listKey: String?,
        list: MutableList<Any?>
    ) {
        builder.append(indent)
        builder.append(listKey)
        if (list.isEmpty()) {
            builder.append(": []\n")
        } else {
            builder.append(":\n")
            list.forEach(Consumer { entry: Any? ->
                when (entry) {
                    is String -> {
                        builder.append(indent)
                        builder.append("  - ")
                        builder.append("\"" + escapeYaml(entry) + "\"")
                        builder.append("\n")
                    }

                    is Int, is Long, is Float, is Double, is Boolean -> {
                        builder.append(indent)
                        builder.append("  - ")
                        builder.append(entry)
                        builder.append("\n")
                    }

                    is MutableMap<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        appendDict(builder, "$indent  ", null, entry as MutableMap<String?, Any?>, true)
                    }

                    else -> {
                        throw RuntimeException(
                            "Invalid value '" +
                                    entry +
                                    "' of type " +
                                    entry!!.javaClass +
                                    " in mapping of ConfigDictSerializable"
                        )
                    }
                }
            })
        }
    }

    private fun appendDict(
        builder: StringBuilder,
        indent: String?,
        dictKey: String?,
        dict: MutableMap<String?, Any?>,
        isListEntry: Boolean
    ) {
        builder.append(indent)
        if (isListEntry) {
            builder.append("-")
        } else {
            builder.append(dictKey)
            builder.append(":")
        }
        if (dict.isEmpty()) {
            builder.append(" {}\n")
        } else {
            builder.append("\n")
            dict
                .entries
                .sortedBy { it.key }
                .forEach { entry: MutableMap.MutableEntry<String?, Any?> ->
                    when (entry.value) {
                        is String -> {
                            builder.append("$indent  ")
                            builder.append(entry.key)
                            builder.append(": ")
                            builder.append("\"" + escapeYaml(entry.value.toString()) + "\"")
                            builder.append("\n")
                        }

                        is Int, is Long, is Float, is Double, is Boolean -> {
                            builder.append("$indent  ")
                            builder.append(entry.key)
                            builder.append(": ")
                            builder.append(entry.value.toString())
                            builder.append("\n")
                        }

                        is MutableMap<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            appendDict(
                                builder,
                                "$indent  ",
                                entry.key,
                                entry.value as MutableMap<String?, Any?>,
                                false
                            )
                        }

                        is MutableList<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            appendList(builder, "$indent  ", entry.key, entry.value as MutableList<Any?>)
                        }

                        else -> {
                            throw RuntimeException(
                                "Invalid value '" +
                                        entry.value +
                                        "' of type " +
                                        entry.value!!.javaClass +
                                        " in mapping of ConfigDictSerializable"
                            )
                        }
                    }
                }
        }
    }

    private fun appendDict(
        builder: StringBuilder,
        indent: String?,
        defaultDefinition: Boolean,
        ser: ConfigDictSerializable
    ) {
        if (defaultDefinition) {
            appendDict(builder, "$indent# ", "Default", ser.toDict()!!, false)
        } else {
            appendDict(builder, indent, basename(), ser.toDict()!!, false)
        }
    }

    override fun generateYaml(
        builder: StringBuilder,
        indent: String,
        existingCompatibleConfig: YamlConfiguration?
    ) {
        appendDescription(builder, indent)
        appendDict(builder, indent, true, def())
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig)
        else
            def()
        appendDict(builder, indent, false, def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)

        if (!yaml.isConfigurationSection(yamlPath())) {
            throw YamlLoadException(
                "Invalid type for yaml path '" + yamlPath() + "', expected configuration section"
            )
        }
    }

    fun loadListFromYaml(rawList: MutableList<*>): ArrayList<Any?> {
        val list = ArrayList<Any?>()
        for (e in rawList) {
            if (e is ConfigurationSection) {
                list.add(loadDictFromYaml(e))
            } else {
                list.add(e)
            }
        }
        return list
    }

    fun loadDictFromYaml(section: ConfigurationSection): HashMap<String?, Any?> {
        val dict = HashMap<String?, Any?>()
        for (subkey in section.getKeys(false)) {
            if (section.isConfigurationSection(subkey)) {
                dict[subkey] = loadDictFromYaml(section.getConfigurationSection(subkey)!!)
            } else if (section.isList(subkey)) {
                dict[subkey] = loadListFromYaml(section.getList(subkey)!!)
            } else if (section.isString(subkey)) {
                dict[subkey] = section.getString(subkey)
            } else if (section.isInt(subkey)) {
                dict[subkey] = section.getInt(subkey)
            } else if (section.isDouble(subkey)) {
                dict[subkey] = section.getDouble(subkey)
            } else if (section.isBoolean(subkey)) {
                dict[subkey] = section.getBoolean(subkey)
            } else if (section.isLong(subkey)) {
                dict[subkey] = section.getLong(subkey)
            } else {
                throw IllegalStateException(
                    "Cannot load dict entry '" + yamlPath() + "." + subkey + "': unknown type"
                )
            }
        }
        return dict
    }

    fun loadFromYaml(yaml: YamlConfiguration): ConfigDictSerializable {
        try {
            val dict = (annotation.cls.java.getDeclaredConstructor().newInstance() as ConfigDictSerializable)
            dict.fromDict(loadDictFromYaml(yaml.getConfigurationSection(yamlPath())!!))
            return dict
        } catch (e: InstantiationException) {
            throw RuntimeException("Could not instanciate storage class for ConfigDict: " + annotation.cls, e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Could not instanciate storage class for ConfigDict: " + annotation.cls, e)
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("Could not instanciate storage class for ConfigDict: " + annotation.cls, e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException("Could not instanciate storage class for ConfigDict: " + annotation.cls, e)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException("Could not instanciate storage class for ConfigDict: " + annotation.cls, e)
        } catch (e: SecurityException) {
            throw RuntimeException("Could not instanciate storage class for ConfigDict: " + annotation.cls, e)
        }
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }
}
