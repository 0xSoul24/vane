package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field
import java.util.function.Function

class ConfigIntField(owner: Any?, field: Field, mapName: Function<String?, String?>, var annotation: ConfigInt) :
    ConfigField<Int?>(owner, field, mapName, "int", annotation.desc) {
    override fun def(): Int {
        val override = overriddenDef()
        return override ?: annotation.def
    }

    override fun metrics(): Boolean {
        val override = overriddenMetrics()
        return override ?: annotation.metrics
    }

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        appendValueRange(
            builder,
            indent,
            annotation.min,
            annotation.max,
            Int.MIN_VALUE,
            Int.MAX_VALUE
        )
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

        if (yaml.get(yamlPath()) !is Number) {
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected int")
        }

        val `val` = yaml.getInt(yamlPath())
        validateIntRange(yamlPath(), `val`, annotation.min, annotation.max)
    }

    fun loadFromYaml(yaml: YamlConfiguration): Int {
        return yaml.getInt(yamlPath())
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.setInt(owner, loadFromYaml(yaml))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }
}
