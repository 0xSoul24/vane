package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigStringList
import org.oddlama.vane.core.YamlLoadException
import java.lang.reflect.Field

class ConfigStringListField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    var annotation: ConfigStringList
) : ConfigField<MutableList<String?>?>(owner, field, mapName, "list of strings", annotation.desc) {

    private fun appendStringListDefinition(builder: StringBuilder?, indent: String?, prefix: String?, def: MutableList<String?>) {
        appendListDefinition<String?>(builder, indent, prefix, def) { b, s ->
            b!!.append("\"${escapeYaml(s)}\"")
        }
    }

    override fun def(): MutableList<String?> = overriddenDef() ?: annotation.def.map { it as String? }.toMutableList()
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        builder.append("$indent# Default:\n")
        appendStringListDefinition(builder, indent, "# ", def())
        builder.append("$indent${basename()}:\n")
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig) else def()
        appendStringListDefinition(builder, indent, "", def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isList(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected list")
        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is String)
                throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected string")
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): MutableList<String?> =
        yaml.getList(yamlPath())!!.map { it as String? }.toMutableList()

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
