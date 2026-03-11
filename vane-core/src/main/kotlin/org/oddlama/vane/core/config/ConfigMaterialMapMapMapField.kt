package org.oddlama.vane.core.config

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.util.MaterialUtil.materialFrom
import org.oddlama.vane.util.StorageUtil.namespacedKey
import java.lang.reflect.Field
import java.util.Comparator.nullsFirst
import java.util.Comparator.naturalOrder

class ConfigMaterialMapMapMapField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    var annotation: ConfigMaterialMapMapMap
) : ConfigField<MutableMap<String?, MutableMap<String?, MutableMap<String?, Material?>?>?>?>(
    owner, field, mapName, "map of string to (map of string to (map of string to material))", annotation.desc
) {
    private fun Material.keyString() = "\"${escapeYaml(key.namespace)}:${escapeYaml(key.key)}\""
    private val nullsFirstNatural = compareBy(nullsFirst(naturalOrder<String>())) { it: Map.Entry<String?, *> -> it.key }

    private fun appendMapDefinition(builder: StringBuilder, indent: String?, prefix: String?, def: MutableMap<String?, MutableMap<String?, MutableMap<String?, Material?>?>?>) {
        def.entries.sortedWith(nullsFirstNatural).forEach { e1 ->
            builder.append("$indent$prefix  ${escapeYaml(e1.key)}:\n")
            e1.value!!.entries.sortedWith(nullsFirstNatural).forEach { e2 ->
                builder.append("$indent$prefix    ${escapeYaml(e2.key)}:\n")
                e2.value!!.entries.sortedWith(nullsFirstNatural).forEach { e3 ->
                    builder.append("$indent$prefix      ${escapeYaml(e3.key)}: ${e3.value!!.keyString()}\n")
                }
            }
        }
    }

    override fun def(): MutableMap<String?, MutableMap<String?, MutableMap<String?, Material?>?>?> =
        overriddenDef() ?: annotation.def.associateTo(mutableMapOf()) { e1 ->
            e1.key to e1.value.associateTo(mutableMapOf()) { e2 ->
                e2.key to e2.value.associateTo(mutableMapOf<String?, Material?>()) { e3 -> e3.key to e3.value }
            }
        }

    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        builder.append("$indent# Default:\n")
        appendMapDefinition(builder, indent, "# ", def())
        builder.append("$indent${basename()}:\n")
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig) else def()
        appendMapDefinition(builder, indent, "", def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isConfigurationSection(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected group")
        for (key1 in yaml.getConfigurationSection(yamlPath())!!.getKeys(false)) {
            val key1Path = "${yamlPath()}.$key1"
            if (!yaml.isConfigurationSection(key1Path))
                throw YamlLoadException("Invalid type for yaml path '$key1Path', expected group")
            for (key2 in yaml.getConfigurationSection(key1Path)!!.getKeys(false)) {
                val key2Path = "$key1Path.$key2"
                if (!yaml.isConfigurationSection(key2Path))
                    throw YamlLoadException("Invalid type for yaml path '$key2Path', expected group")
                for (key3 in yaml.getConfigurationSection(key2Path)!!.getKeys(false)) {
                    val key3Path = "$key2Path.$key3"
                    if (!yaml.isString(key3Path))
                        throw YamlLoadException("Invalid type for yaml path '$key3Path', expected string")
                    val str = yaml.getString(key3Path)!!
                    val (ns, key) = requireNamespacedKeyParts(key3Path, str, "material")
                    materialFrom(namespacedKey(ns, key))
                        ?: throw YamlLoadException("Invalid material entry in list '$key3Path': '$str' does not exist")
                }
            }
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): MutableMap<String?, MutableMap<String?, MutableMap<String?, Material?>?>?> =
        yaml.getConfigurationSection(yamlPath())!!.getKeys(false).associateTo(mutableMapOf()) { key1 ->
            val key1Path = "${yamlPath()}.$key1"
            key1 to yaml.getConfigurationSection(key1Path)!!.getKeys(false).associateTo(mutableMapOf()) { key2 ->
                val key2Path = "$key1Path.$key2"
                key2 to yaml.getConfigurationSection(key2Path)!!.getKeys(false).associateTo(mutableMapOf<String?, Material?>()) { key3 ->
                    val key3Path = "$key2Path.$key3"
                    val (ns, key) = requireNamespacedKeyParts(key3Path, yaml.getString(key3Path)!!, "material")
                    key3 to materialFrom(namespacedKey(ns, key))
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
