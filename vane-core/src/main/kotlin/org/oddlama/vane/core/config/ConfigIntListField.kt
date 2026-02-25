package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigIntList
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field
import java.util.function.Function

class ConfigIntListField(
    owner: Any?,
    field: Field,
    mapName: Function<String?, String?>,
    var annotation: ConfigIntList
) : ConfigField<MutableList<Int?>?>(owner, field, mapName, "int list", annotation.desc) {
    private fun appendIntListDefinition(
        builder: StringBuilder?,
        indent: String?,
        prefix: String?,
        def: MutableList<Int?>
    ) {
        appendListDefinition<Int?>(
            builder,
            indent,
            prefix,
            def
        ) { b: StringBuilder?, i: Int? -> b!!.append(i!!) }
    }

    override fun def(): MutableList<Int?> {
        val override = overriddenDef()
        return override ?: annotation.def.map { it as Int? }.toMutableList()
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

        // Default
        builder.append(indent)
        builder.append("# Default:\n")
        appendIntListDefinition(builder, indent, "# ", def())

        // Definition
        builder.append(indent)
        builder.append(basename())
        builder.append(":\n")
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig)
        else
            def()
        appendIntListDefinition(builder, indent, "", def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)

        if (!yaml.isList(yamlPath())) {
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected list")
        }

        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is Number) {
                throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected int")
            }

            val `val` = obj.toInt()
            // Usar validación centralizada
            validateIntRange(yamlPath(), `val`, annotation.min, annotation.max)
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): MutableList<Int?> {
        val list = ArrayList<Int?>()
        for (obj in yaml.getList(yamlPath())!!) {
            list.add((obj as Number).toInt())
        }
        return list
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }
}