package org.oddlama.vane.core.config

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigMaterialMapEntry
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapEntry
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMapEntry
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.util.MaterialUtil.materialFrom
import org.oddlama.vane.util.StorageUtil.namespacedKey
import java.lang.reflect.Field
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

class ConfigMaterialMapMapMapField(
    owner: Any?,
    field: Field,
    mapName: Function<String?, String?>,
    var annotation: ConfigMaterialMapMapMap
) : ConfigField<MutableMap<String?, MutableMap<String?, MutableMap<String?, Material?>?>?>?>(
    owner,
    field,
    mapName,
    "map of string to (map of string to (map of string to material))",
    annotation.desc
) {
    private fun appendMapDefinition(
        builder: StringBuilder,
        indent: String?,
        prefix: String?,
        def: MutableMap<String?, MutableMap<String?, MutableMap<String?, Material?>?>?>
    ) {
        def.entries
            .sortedWith(compareBy(nullsFirst(naturalOrder())) { it.key })
            .forEach { e1 ->
                builder.append(indent)
                builder.append(prefix)
                builder.append("  ")
                builder.append(escapeYaml(e1.key))
                builder.append(":\n")
                e1.value!!.entries
                    .sortedWith(compareBy(nullsFirst(naturalOrder())) { it.key })
                    .forEach { e2 ->
                        builder.append(indent)
                        builder.append(prefix)
                        builder.append("    ")
                        builder.append(escapeYaml(e2.key))
                        builder.append(":\n")
                        e2.value!!.entries
                            .sortedWith(compareBy(nullsFirst(naturalOrder())) { it.key })
                            .forEach { e3 ->
                                builder.append(indent)
                                builder.append(prefix)
                                builder.append("      ")
                                builder.append(escapeYaml(e3.key))
                                builder.append(": \"")
                                builder.append(escapeYaml(e3.value!!.getKey().namespace))
                                builder.append(":")
                                builder.append(escapeYaml(e3.value!!.getKey().key))
                                builder.append("\"\n")
                            }
                    }
            }
    }

    override fun def(): MutableMap<String?, MutableMap<String?, MutableMap<String?, Material?>?>?> {
        val override = overriddenDef()
        if (override != null) {
            return override
        } else {
            @Suppress("UNCHECKED_CAST")
            return Arrays.stream(annotation.def).collect(
                Collectors.toMap(ConfigMaterialMapMapMapEntry::key, Function { e1: ConfigMaterialMapMapMapEntry? ->
                    Arrays.stream(e1!!.value).collect(
                        Collectors.toMap(ConfigMaterialMapMapEntry::key, Function { e2: ConfigMaterialMapMapEntry? ->
                            Arrays.stream(e2!!.value).collect(
                                Collectors.toMap(
                                    ConfigMaterialMapEntry::key,
                                    Function { e3: ConfigMaterialMapEntry? -> e3!!.value })
                            ) as MutableMap<String?, Material?>?
                        })
                    ) as MutableMap<String?, MutableMap<String?, Material?>?>?
                })
            ) as MutableMap<String?, MutableMap<String?, MutableMap<String?, Material?>?>?>
        }
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
        appendMapDefinition(builder, indent, "# ", def())

        // Definition
        builder.append(indent)
        builder.append(basename())
        builder.append(":\n")
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig)
        else
            def()
        appendMapDefinition(builder, indent, "", def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)

        if (!yaml.isConfigurationSection(yamlPath())) {
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected group")
        }

        for (key1 in yaml.getConfigurationSection(yamlPath())!!.getKeys(false)) {
            val key1Path = yamlPath() + "." + key1
            if (!yaml.isConfigurationSection(key1Path)) {
                throw YamlLoadException("Invalid type for yaml path '$key1Path', expected group")
            }

            for (key2 in yaml.getConfigurationSection(key1Path)!!.getKeys(false)) {
                val key2Path = "$key1Path.$key2"
                if (!yaml.isConfigurationSection(key2Path)) {
                    throw YamlLoadException("Invalid type for yaml path '$key2Path', expected group")
                }

                for (key3 in yaml.getConfigurationSection(key2Path)!!.getKeys(false)) {
                    val key3Path = "$key2Path.$key3"
                    if (!yaml.isString(key3Path)) {
                        throw YamlLoadException("Invalid type for yaml path '$key3Path', expected string")
                    }

                    val str = yaml.getString(key3Path)
                    val split: Array<String?> = str!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (split.size != 2) {
                        throw YamlLoadException(
                            "Invalid material entry in list '" +
                                    key3Path +
                                    "': '" +
                                    str +
                                    "' is not a valid namespaced key"
                        )
                    }

                    materialFrom(namespacedKey(split[0]!!, split[1]!!)) ?: throw YamlLoadException(
                        "Invalid material entry in list '$key3Path': '$str' does not exist"
                    )
                }
            }
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): MutableMap<String?, MutableMap<String?, MutableMap<String?, Material?>?>?> {
        val map1 = HashMap<String?, MutableMap<String?, MutableMap<String?, Material?>?>?>()
        for (key1 in yaml.getConfigurationSection(yamlPath())!!.getKeys(false)) {
            val key1Path = yamlPath() + "." + key1
            val map2 = HashMap<String?, MutableMap<String?, Material?>?>()
            map1[key1] = map2
            for (key2 in yaml.getConfigurationSection(key1Path)!!.getKeys(false)) {
                val key2Path = "$key1Path.$key2"
                val map3 = HashMap<String?, Material?>()
                map2[key2] = map3
                for (key3 in yaml.getConfigurationSection(key2Path)!!.getKeys(false)) {
                    val key3Path = "$key2Path.$key3"
                    val split: Array<String?> =
                        yaml.getString(key3Path)!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    map3[key3] = materialFrom(namespacedKey(split[0]!!, split[1]!!))
                }
            }
        }
        return map1
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }
}
