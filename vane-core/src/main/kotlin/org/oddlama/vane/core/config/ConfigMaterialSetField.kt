package org.oddlama.vane.core.config

import org.bstats.bukkit.Metrics
import org.bstats.charts.AdvancedPie
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigMaterialSet
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.util.MaterialUtil.materialFrom
import org.oddlama.vane.util.StorageUtil.namespacedKey
import java.lang.reflect.Field

class ConfigMaterialSetField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    var annotation: ConfigMaterialSet
) : ConfigField<MutableSet<Material>?>(owner, field, mapName, "set of materials", annotation.desc) {

    private fun Material.keyString() = "\"${escapeYaml(key.namespace)}:${escapeYaml(key.key)}\""

    private fun appendMaterialSetDefinition(builder: StringBuilder?, indent: String?, prefix: String?, def: MutableSet<Material>) {
        appendListDefinition(builder, indent, prefix, def) { b, m -> b!!.append(m!!.keyString()) }
    }

    override fun def(): MutableSet<Material> = overriddenDef() ?: annotation.def.toMutableSet()
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    override fun registerMetrics(metrics: Metrics?) {
        if (metrics == null || !this.metrics()) return
        metrics.addCustomChart(AdvancedPie(yamlPath()) {
            get()!!.associate { it.key.toString() to 1 }
        })
    }

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        builder.append("$indent# Default:\n")
        appendMaterialSetDefinition(builder, indent, "# ", def())
        builder.append("$indent${basename()}:\n")
        val def: MutableSet<Material> = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig) else def()
        appendMaterialSetDefinition(builder, indent, "", def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isList(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected list")
        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is String)
                throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected string")
            val (ns, key) = requireNamespacedKeyParts(yamlPath(), obj, "material")
            materialFrom(namespacedKey(ns, key))
                ?: throw YamlLoadException("Invalid material entry in list '${yamlPath()}': '$obj' does not exist")
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): MutableSet<Material> =
        yaml.getList(yamlPath())!!.mapTo(mutableSetOf()) { obj ->
            val (ns, key) = requireNamespacedKeyParts(yamlPath(), obj as String, "material")
            materialFrom(namespacedKey(ns, key))!!
        }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
