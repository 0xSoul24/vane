package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field

class ConfigBooleanField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    private val annotation: ConfigBoolean
) : ConfigField<Boolean?>(owner, field, mapName, "boolean", annotation.desc) {

    override fun def(): Boolean = overriddenDef() ?: annotation.def
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        appendDefaultValue(builder, indent, def())
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig)
        else
            def()
        appendFieldDefinition(builder, indent, def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isBoolean(yamlPath())) {
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected boolean")
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): Boolean {
        return yaml.getBoolean(yamlPath())
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.setBoolean(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
