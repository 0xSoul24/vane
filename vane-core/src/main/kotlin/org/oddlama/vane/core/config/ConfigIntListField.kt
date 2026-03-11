package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigIntList
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field

class ConfigIntListField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    var annotation: ConfigIntList
) : ConfigField<MutableList<Int?>?>(owner, field, mapName, "int list", annotation.desc) {

    private fun appendIntListDefinition(builder: StringBuilder?, indent: String?, prefix: String?, def: MutableList<Int?>) {
        appendListDefinition<Int?>(builder, indent, prefix, def) { b, i -> b!!.append(i!!) }
    }

    override fun def(): MutableList<Int?> = overriddenDef() ?: annotation.def.map { it as Int? }.toMutableList()
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        appendValueRange(builder, indent, annotation.min, annotation.max, Int.MIN_VALUE, Int.MAX_VALUE)
        builder.append("$indent# Default:\n")
        appendIntListDefinition(builder, indent, "# ", def())
        builder.append("$indent${basename()}:\n")
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig) else def()
        appendIntListDefinition(builder, indent, "", def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isList(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected list")
        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is Number)
                throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected int")
            validateIntRange(yamlPath(), obj.toInt(), annotation.min, annotation.max)
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): MutableList<Int?> =
        yaml.getList(yamlPath())!!.map { (it as Number).toInt() as Int? }.toMutableList()

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}