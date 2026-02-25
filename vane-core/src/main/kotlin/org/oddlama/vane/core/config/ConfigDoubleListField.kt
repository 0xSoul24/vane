package org.oddlama.vane.core.config

import org.apache.commons.lang3.ArrayUtils
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigDoubleList
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field
import java.util.function.Function

class ConfigDoubleListField(
    owner: Any?,
    field: Field,
    mapName: Function<String?, String?>,
    var annotation: ConfigDoubleList
) : ConfigField<MutableList<Double?>?>(owner, field, mapName, "double list", annotation.desc) {
    private fun appendDoubleListDefinition(
        builder: StringBuilder?,
        indent: String?,
        prefix: String?,
        def: MutableList<Double?>
    ) {
        appendListDefinition<Double?>(
            builder,
            indent,
            prefix,
            def
        ) { b: StringBuilder?, d: Double? -> b!!.append(d!!) }
    }

    override fun def(): MutableList<Double?> {
        val override = overriddenDef()
        return override ?: ArrayUtils.toObject(annotation.def).toMutableList()
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

        // Default
        builder.append(indent)
        builder.append("# Default:\n")
        appendDoubleListDefinition(builder, indent, "# ", def())

        // Definition
        builder.append(indent)
        builder.append(basename())
        builder.append(":\n")
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig)
        else
            def()
        appendDoubleListDefinition(builder, indent, "", def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)

        if (!yaml.isList(yamlPath())) {
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected list")
        }

        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is Number) {
                throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected double")
            }

            // Usar el elemento de la lista en vez de leer de nuevo la ruta
            val value = obj.toDouble()
            validateDoubleRange(yamlPath(), value, annotation.min, annotation.max)
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): MutableList<Double?> {
        val list = ArrayList<Double?>()
        for (obj in yaml.getList(yamlPath())!!) {
            list.add((obj as Number).toDouble())
        }
        return list
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.", e)
        }
    }
}