package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field

class ConfigStringField(owner: Any?, field: Field, mapName: (String?) -> String?, var annotation: ConfigString) :
    ConfigField<String?>(owner, field, mapName, "string", annotation.desc) {

    override fun def(): String = overriddenDef() ?: annotation.def
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        appendDefaultValue(builder, indent, "\"${escapeYaml(def())}\"")
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig) else def()
        appendFieldDefinition(builder, indent, "\"${escapeYaml(def)}\"")
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isString(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected string")
    }

    fun loadFromYaml(yaml: YamlConfiguration): String = yaml.getString(yamlPath())!!

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
