package org.oddlama.vane.core.config

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigMaterial
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.util.MaterialUtil.materialFrom
import org.oddlama.vane.util.StorageUtil.namespacedKey
import java.lang.reflect.Field
import java.util.function.Function

class ConfigMaterialField(
    owner: Any?,
    field: Field,
    mapName: Function<String?, String?>,
    var annotation: ConfigMaterial
) : ConfigField<Material?>(owner, field, mapName, "material", annotation.desc) {
    override fun def(): Material {
        val override = overriddenDef()
        return override ?: annotation.def
    }

    override fun metrics(): Boolean {
        val override = overriddenMetrics()
        return override ?: annotation.metrics
    }

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        appendDefaultValue(
            builder,
            indent,
            "\"" + escapeYaml(def().getKey().namespace) + ":" + escapeYaml(def().getKey().key) + "\""
        )
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig)
        else
            def()
        appendFieldDefinition(
            builder,
            indent,
            "\"" + escapeYaml(def.getKey().namespace) + ":" + escapeYaml(def.getKey().key) + "\""
        )
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)

        if (!yaml.isString(yamlPath())) {
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected string")
        }

        val str = yaml.getString(yamlPath())
        val parts = requireNamespacedKeyParts(yamlPath(), str, "material")

        val mat = materialFrom(namespacedKey(parts.first, parts.second)) ?: throw YamlLoadException(
            "Invalid material entry in list '" + yamlPath() + "': '" + str + "' does not exist"
        )
    }

    fun loadFromYaml(yaml: YamlConfiguration): Material {
        val str = yaml.getString(yamlPath())
        val parts = requireNamespacedKeyParts(yamlPath(), str, "material")
        return materialFrom(namespacedKey(parts.first, parts.second))!!
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }
}
