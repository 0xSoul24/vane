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
import java.util.*
import java.util.function.Function

class ConfigMaterialSetField(
    owner: Any?,
    field: Field,
    mapName: Function<String?, String?>,
    var annotation: ConfigMaterialSet
) : ConfigField<MutableSet<Material>?>(owner, field, mapName, "set of materials", annotation.desc) {
    private fun appendMaterialSetDefinition(
        builder: StringBuilder?,
        indent: String?,
        prefix: String?,
        def: MutableSet<Material>
    ) {
        appendListDefinition(builder, indent, prefix, def) { b: StringBuilder?, m: Material? ->
            b!!.append("\"")
            b.append(escapeYaml(m!!.getKey().namespace))
            b.append(":")
            b.append(escapeYaml(m.getKey().key))
            b.append("\"")
        }
    }

    override fun def(): MutableSet<Material> {
        val override = overriddenDef()
        return override ?: annotation.def.toMutableSet()
    }

    override fun metrics(): Boolean {
        val override = overriddenMetrics()
        return override ?: annotation.metrics
    }

    override fun registerMetrics(metrics: Metrics?) {
        if (metrics == null) return
        if (!this.metrics()) return
        metrics.addCustomChart(
            AdvancedPie(yamlPath()) {
                val values = HashMap<String?, Int?>()
                for (v in get()!!) {
                    values[v.getKey().toString()] = 1
                }
                values
            }
        )
    }

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)

        // Default
        builder.append(indent)
        builder.append("# Default:\n")
        appendMaterialSetDefinition(builder, indent, "# ", def())

        // Definition
        builder.append(indent)
        builder.append(basename())
        builder.append(":\n")
        val def: MutableSet<Material> = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig)
        else
            def()
        // Ensure the set contains non-null Material elements (avoid captured wildcard of Material?)
        val defNonNull: MutableSet<Material> = def.toList().toMutableSet()
        appendMaterialSetDefinition(builder, indent, "", defNonNull)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)

        if (!yaml.isList(yamlPath())) {
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected list")
        }

        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is String) {
                throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected string")
            }

            val split: Array<String?> = obj.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (split.size != 2) {
                throw YamlLoadException(
                    "Invalid material entry in list '" + yamlPath() + "': '" + obj + "' is not a valid namespaced key"
                )
            }

            materialFrom(namespacedKey(split[0]!!, split[1]!!)) ?: throw YamlLoadException(
                "Invalid material entry in list '" + yamlPath() + "': '" + obj + "' does not exist"
            )
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): MutableSet<Material> {
        val set = HashSet<Material>()
        for (obj in yaml.getList(yamlPath())!!) {
            val split: Array<String?> =
                (obj as String).split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            set.add(materialFrom(namespacedKey(split[0]!!, split[1]!!))!!)
        }
        return set
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }
}
