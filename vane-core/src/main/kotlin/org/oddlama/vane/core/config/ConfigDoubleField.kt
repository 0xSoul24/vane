package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field
import java.util.function.Function
import kotlin.Any
import kotlin.Boolean
import kotlin.RuntimeException
import kotlin.String
import kotlin.Throws

class ConfigDoubleField(owner: Any?, field: Field, mapName: Function<String?, String?>, var annotation: ConfigDouble) :
    ConfigField<Double?>(owner, field, mapName, "double", annotation.desc) {
    override fun def(): Double {
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
            Double.NaN,
            Double.NaN
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

        if (!yaml.isDouble(yamlPath())) {
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected double")
        }

        // Usar validación centralizada
        validateDoubleRange(yamlPath(), yaml.getDouble(yamlPath()), annotation.min, annotation.max)
    }

    fun loadFromYaml(yaml: YamlConfiguration): Double {
        return yaml.getDouble(yamlPath())
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.setDouble(owner, loadFromYaml(yaml))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }
}
