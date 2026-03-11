package org.oddlama.vane.core.config

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigMaterial
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.util.MaterialUtil.materialFrom
import org.oddlama.vane.util.StorageUtil.namespacedKey
import java.lang.reflect.Field

class ConfigMaterialField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    var annotation: ConfigMaterial
) : ConfigField<Material?>(owner, field, mapName, "material", annotation.desc) {

    override fun def(): Material = overriddenDef() ?: annotation.def
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    private fun Material.keyString() = "\"${escapeYaml(key.namespace)}:${escapeYaml(key.key)}\""

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        appendDefaultValue(builder, indent, def().keyString())
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig) else def()
        appendFieldDefinition(builder, indent, def.keyString())
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isString(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected string")
        val str = yaml.getString(yamlPath())
        val (ns, key) = requireNamespacedKeyParts(yamlPath(), str, "material")
        materialFrom(namespacedKey(ns, key))
            ?: throw YamlLoadException("Invalid material entry in list '${yamlPath()}': '$str' does not exist")
    }

    fun loadFromYaml(yaml: YamlConfiguration): Material {
        val str = yaml.getString(yamlPath())
        val (ns, key) = requireNamespacedKeyParts(yamlPath(), str, "material")
        return materialFrom(namespacedKey(ns, key))!!
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
