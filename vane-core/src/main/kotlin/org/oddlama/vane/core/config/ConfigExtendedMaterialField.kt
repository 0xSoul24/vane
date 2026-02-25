package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.material.ExtendedMaterial
import org.oddlama.vane.core.material.ExtendedMaterial.Companion.from
import org.oddlama.vane.util.StorageUtil.namespacedKey
import java.lang.reflect.Field
import java.util.function.Function

class ConfigExtendedMaterialField(
    owner: Any?,
    field: Field,
    mapName: Function<String?, String?>,
    var annotation: ConfigExtendedMaterial
) : ConfigField<ExtendedMaterial?>(owner, field, mapName, "extended material", annotation.desc) {
    override fun def(): ExtendedMaterial? {
        val override = overriddenDef()
        if (override != null) {
            return override
        } else {
            val parts = requireNamespacedKeyParts(yamlPath(), annotation.def, "extended material")
            return from(namespacedKey(parts.first, parts.second))
        }
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
            "\"" + escapeYaml(def()!!.key().namespace) + ":" + escapeYaml(def()!!.key().key) + "\""
        )
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig)
        else
            def()
        appendFieldDefinition(
            builder,
            indent,
            "\"" + escapeYaml(def!!.key().namespace) + ":" + escapeYaml(def.key().key) + "\""
        )
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)

        if (!yaml.isString(yamlPath())) {
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected string")
        }

        val str = yaml.getString(yamlPath())
        val parts = requireNamespacedKeyParts(yamlPath(), str, "extended material")

        val mat = from(namespacedKey(parts.first, parts.second)) ?: throw YamlLoadException(
            "Invalid extended material entry in list '" + yamlPath() + "': '" + str + "' does not exist"
        )
    }

    fun loadFromYaml(yaml: YamlConfiguration): ExtendedMaterial? {
        val str = yaml.getString(yamlPath())
        val parts = requireNamespacedKeyParts(yamlPath(), str, "extended material")
        return from(namespacedKey(parts.first, parts.second))
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }
}
