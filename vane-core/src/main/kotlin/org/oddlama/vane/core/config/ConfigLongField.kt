package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field
import java.util.function.Function

class ConfigLongField(owner: Any?, field: Field, mapName: Function<String?, String?>, var annotation: ConfigLong) :
    ConfigField<Long?>(owner, field, mapName, "long", annotation.desc) {
    override fun def(): Long {
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
            Long.MIN_VALUE,
            Long.MAX_VALUE
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
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected long")
        }

        val `val` = yaml.getLong(yamlPath())
        if (annotation.min != Long.MIN_VALUE && `val` < annotation.min) {
            throw YamlLoadException(
                "Configuration '" + yamlPath() + "' has an invalid value: Value must be >= " + annotation.min
            )
        }
        if (annotation.max != Long.MAX_VALUE && `val` > annotation.max) {
            throw YamlLoadException(
                "Configuration '" + yamlPath() + "' has an invalid value: Value must be <= " + annotation.max
            )
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): Long {
        return yaml.getLong(yamlPath())
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.setLong(owner, loadFromYaml(yaml))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }
}
