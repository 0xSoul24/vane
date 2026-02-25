package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.config.ConfigItemStack
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.util.MaterialUtil.materialFrom
import org.oddlama.vane.util.StorageUtil.namespacedKey
import java.lang.reflect.Field
import java.util.function.Function

class ConfigItemStackField(
    owner: Any?,
    field: Field,
    mapName: Function<String?, String?>,
    var annotation: ConfigItemStack
) : ConfigField<ItemStack?>(owner, field, mapName, "item stack", annotation.desc) {
    private fun appendItemStackDefinition(builder: StringBuilder, indent: String?, prefix: String?, def: ItemStack) {
        // Material
        builder.append(indent)
        builder.append(prefix)
        builder.append("  material: ")
        val material =
            "\"" +
                    escapeYaml(def.type.getKey().namespace) +
                    ":" +
                    escapeYaml(def.type.getKey().key) +
                    "\""
        builder.append(material)
        builder.append("\n")

        // Amount
        if (def.amount != 1) {
            builder.append(indent)
            builder.append(prefix)
            builder.append("  amount: ")
            builder.append(def.amount)
            builder.append("\n")
        }
    }

    override fun def(): ItemStack {
        val override = overriddenDef()
        return override ?: ItemStack(annotation.def.type, annotation.def.amount)
    }

    override fun metrics(): Boolean {
        val override = overriddenMetrics()
        return override ?: annotation.metrics
    }

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)

        // Default
        builder.append(indent)
        builder.append("# Default:\n")
        appendItemStackDefinition(builder, indent, "# ", def())

        // Definition
        builder.append(indent)
        builder.append(basename())
        builder.append(":\n")
        val def = if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
            loadFromYaml(existingCompatibleConfig)
        else
            def()
        appendItemStackDefinition(builder, indent, "", def)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)

        if (!yaml.isConfigurationSection(yamlPath())) {
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected group")
        }

        for (varKey in yaml.getConfigurationSection(yamlPath())!!.getKeys(false)) {
            val varPath = yamlPath() + "." + varKey
            when (varKey) {
                "material" -> {
                    if (!yaml.isString(varPath)) {
                        throw YamlLoadException("Invalid type for yaml path '$varPath', expected list")
                    }

                    val str = yaml.getString(varPath)
                    val split: Array<String?> = str!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (split.size != 2) {
                        throw YamlLoadException(
                            "Invalid material for yaml path '" +
                                    yamlPath() +
                                    "': '" +
                                    str +
                                    "' is not a valid namespaced key"
                        )
                    }
                }

                "amount" -> {
                    if (yaml.get(varPath) !is Number) {
                        throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected int")
                    }
                    val `val` = yaml.getInt(yamlPath())
                    if (`val` < 0) {
                        throw YamlLoadException("Invalid value for yaml path '" + yamlPath() + "' Must be >= 0")
                    }
                }
            }
        }
    }

    fun loadFromYaml(yaml: YamlConfiguration): ItemStack {
        var materialStr: String? = ""
        var amount = 1
        for (varKey in yaml.getConfigurationSection(yamlPath())!!.getKeys(false)) {
            val varPath = yamlPath() + "." + varKey
            when (varKey) {
                "material" -> {
                    amount = 0
                    materialStr = yaml.getString(varPath)
                }

                "amount" -> {
                    amount = yaml.getInt(varPath)
                }
            }
        }

        val split: Array<String?> = materialStr!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val material = materialFrom(namespacedKey(split[0]!!, split[1]!!))
        return ItemStack(material!!, amount)
    }

    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '" + field.name + "'. This is a bug.")
        }
    }
}
