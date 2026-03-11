package org.oddlama.vane.core.config

import org.apache.commons.lang3.ArrayUtils
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigDoubleList
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field

class ConfigDoubleListField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    var annotation: ConfigDoubleList
) : ConfigField<MutableList<Double?>?>(owner, field, mapName, "double list", annotation.desc) {

    private fun appendDoubleListDefinition(builder: StringBuilder?, indent: String?, prefix: String?, def: MutableList<Double?>) {
        appendListDefinition<Double?>(builder, indent, prefix, def) { b, d -> b!!.append(d!!) }
    }

    override fun def(): MutableList<Double?> = overriddenDef() ?: ArrayUtils.toObject(annotation.def).toMutableList()
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        appendValueRange(builder, indent, annotation.min, annotation.max, Double.NaN, Double.NaN)
        builder.append("$indent# Default:\n")
        appendDoubleListDefinition(builder, indent, "# ", def())
        builder.append("$indent${basename()}:\n")
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig) else def()
        appendDoubleListDefinition(builder, indent, "", def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isList(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected list")
        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is Number)
                throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected double")
            validateDoubleRange(yamlPath(), obj.toDouble(), annotation.min, annotation.max)
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): MutableList<Double?> =
        yaml.getList(yamlPath())!!.map { (it as Number).toDouble() as Double? }.toMutableList()

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}