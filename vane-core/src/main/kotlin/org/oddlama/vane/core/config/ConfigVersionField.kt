package org.oddlama.vane.core.config

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigVersion
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Module
import java.lang.reflect.Field
import java.util.function.Function

class ConfigVersionField(
    owner: Any?,
    field: Field,
    mapName: Function<String?, String?>,
    var annotation: ConfigVersion?
) : ConfigField<Long?>(
    owner,
    field,
    mapName,
    "version id",
    "DO NOT CHANGE! The version of this config file. Used to determine if the config needs to be updated."
) {
    init {
        // Version field should be at the bottom
        this.sortPriority = 100
    }

    override fun def(): Long? {
        return null
    }

    override fun metrics(): Boolean {
        return true
    }

    override fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?) {
        appendDescription(builder, indent)
        appendFieldDefinition(builder, indent, (owner as Module<*>).annotation.configVersion)
    }

    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)

        if (yaml.get(yamlPath()) !is Number) {
            throw YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected long")
        }

        val `val` = yaml.getLong(yamlPath())
        if (`val` < 1) {
            throw YamlLoadException("Configuration '" + yamlPath() + "' has an invalid value: Value must be >= 1")
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
